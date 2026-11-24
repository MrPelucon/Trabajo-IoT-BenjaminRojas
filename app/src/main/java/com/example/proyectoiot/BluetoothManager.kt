package com.example.proyectoiot

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

/** Una sola conexión Bluetooth Classic (SPP) compartida entre las Activities. */
object BluetoothManager {
    enum class EstadoConexion {
        SIN_CONFIGURAR, ESPERANDO_BLUETOOTH, CONECTANDO, CONECTADO, DESCONECTADO
    }

    enum class Control(val encender: String, val apagar: String) {
        LED1("A", "B"), LED2("C", "D"), SENSOR("E", "F");
        fun comando(activo: Boolean) = if (activo) encender else apagar
    }

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val PREFS = "configuracion_hc06"
    private const val CLAVE_MAC = "mac_hc06"
    private const val REINTENTO_MS = 3000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val observadoresLock = Any()
    private val observadoresEstado = mutableSetOf<(EstadoConexion) -> Unit>()
    private val observadoresDatos = mutableSetOf<(String) -> Unit>()
    private val escritor = Executors.newSingleThreadExecutor()
    private val hiloLock = Any()
    private val datosLock = Any()

    @Volatile private var ejecutar = false
    @Volatile private var hiloConexion: Thread? = null
    @Volatile private var direccionMac: String? = null
    // Solo conserva applicationContext, cuyo ciclo de vida coincide con el proceso.
    @SuppressLint("StaticFieldLeak")
    @Volatile private var contexto: Context? = null
    @Volatile private var socketActivo: BluetoothSocket? = null
    @Volatile private var receptorRegistrado = false

    @Volatile var estadoActual = EstadoConexion.SIN_CONFIGURAR
        private set
    @Volatile var ultimoMensaje: String? = null
        private set
    @Volatile var estadoPir: Boolean? = null
        private set
    @Volatile var sensorConfirmado: Boolean? = null
        private set
    private var ultimoEventoPir: PirMessageParser.Evento? = null
    // El firmware original solo envía cambios; el nuevo usa PIR:0/1 periódicos.
    @Volatile var lecturasPeriodicas = false
        private set
    @Volatile var ultimaDeteccionMs: Long? = null
        private set
    @Volatile var ultimaLecturaPirMs: Long? = null
        private set

    private fun almacenamiento(): IoTStorage? {
        val app = contexto ?: return null
        val mac = direccionMac ?: return null
        return IoTStorage(app, mac)
    }

    fun estadoControl(control: Control): Boolean? = synchronized(datosLock) {
        almacenamiento()?.control(control)
    }

    /** Guarda la configuración deseada; A-F no confirman el estado físico. */
    fun configurarControl(control: Control, activo: Boolean): Boolean = synchronized(datosLock) {
        val conexion = socketActivo ?: return@synchronized false
        if (estadoActual != EstadoConexion.CONECTADO) return@synchronized false
        val datos = almacenamiento() ?: return@synchronized false
        datos.guardarControl(control, activo)
        if (control == Control.SENSOR) {
            estadoPir = null
            sensorConfirmado = null
            ultimaLecturaPirMs = null
        }
        encolarComando(conexion, control.comando(activo))
        true
    }

    internal fun historialPir(): List<IoTStorage.EventoPir> = synchronized(datosLock) {
        almacenamiento()?.historial() ?: emptyList()
    }

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val adaptador = obtenerAdaptador(context)
                    if (!tienePermiso(context) || adaptador?.isEnabled != true) cerrarSocketActivo()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    try {
                        @Suppress("DEPRECATION")
                        val equipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (equipo?.address == direccionMac) cerrarSocketActivo()
                    } catch (_: SecurityException) {
                        cerrarSocketActivo()
                    }
                }
            }
        }
    }

    private fun obtenerAdaptador(context: Context): BluetoothAdapter? =
        context.getSystemService(AndroidBluetoothManager::class.java)?.adapter

    private fun tienePermiso(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun dispositivosVinculados(context: Context): List<BluetoothDevice> {
        if (!tienePermiso(context)) return emptyList()
        return obtenerAdaptador(context)?.bondedDevices?.sortedBy { it.name ?: it.address }
            ?: emptyList()
    }

    /** Reutiliza la MAC guardada solo si el dispositivo sigue vinculado. */
    @SuppressLint("MissingPermission")
    fun reconectarGuardado(context: Context): Boolean {
        if (!tienePermiso(context)) return false
        val guardada = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE_MAC, null) ?: return false
        if (dispositivosVinculados(context).none { it.address == guardada }) return false
        conectar(context, guardada)
        return true
    }

    @SuppressLint("MissingPermission")
    fun conectar(context: Context, mac: String) {
        require(BluetoothAdapter.checkBluetoothAddress(mac)) { "Dirección MAC no válida" }
        require(tienePermiso(context)) { "Falta permiso BLUETOOTH_CONNECT" }
        val app = context.applicationContext
        contexto = app
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(CLAVE_MAC, mac)
        }
        registrarReceptor(app)

        synchronized(datosLock) {
            if (direccionMac != mac) {
                cerrarSocketActivo()
                direccionMac = mac
                ultimaDeteccionMs = almacenamiento()?.ultimaDeteccion()
                actualizarEstado(EstadoConexion.DESCONECTADO, forzar = true)
            }
        }
        synchronized(hiloLock) {
            if (hiloConexion?.isAlive != true) {
                ejecutar = true
                hiloConexion = Thread(::bucleConexion, "HC06-reconexion").apply {
                    isDaemon = true
                    start()
                }
            }
        }
    }

    private fun registrarReceptor(context: Context) {
        synchronized(hiloLock) {
            if (receptorRegistrado) return
            val filtro = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            ContextCompat.registerReceiver(
                context, receptor, filtro, ContextCompat.RECEIVER_EXPORTED
            )
            receptorRegistrado = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun bucleConexion() {
        while (ejecutar) {
            val app = contexto ?: break
            val mac = direccionMac ?: break
            val adaptador = obtenerAdaptador(app)
            if (!tienePermiso(app) || adaptador?.isEnabled != true) {
                actualizarEstado(EstadoConexion.ESPERANDO_BLUETOOTH)
                esperarReintento()
                continue
            }

            var candidato: BluetoothSocket? = null
            try {
                actualizarEstado(EstadoConexion.CONECTANDO)
                candidato = adaptador.getRemoteDevice(mac)
                    .createRfcommSocketToServiceRecord(sppUuid)
                val asignado = synchronized(datosLock) {
                    if (!ejecutar || direccionMac != mac) false
                    else {
                        socketActivo = candidato
                        true
                    }
                }
                if (!asignado) continue
                candidato.connect() // Operación bloqueante, nunca en el hilo de UI.
                val listo = synchronized(datosLock) {
                    if (!ejecutar || direccionMac != mac || socketActivo !== candidato) false
                    else {
                        val datos = almacenamiento()
                        // Reaplica solo las preferencias que el usuario haya establecido.
                        Control.entries.forEach { control ->
                            datos?.control(control)?.let { activo ->
                                encolarComando(candidato, control.comando(activo))
                            }
                        }
                        actualizarEstado(EstadoConexion.CONECTADO)
                        true
                    }
                }
                if (!listo) continue
                leerMensajes(candidato, mac)
            } catch (_: IOException) {
                // Al perder el enlace, el ciclo continuará intentando la conexión.
            } catch (_: SecurityException) {
                // Si revocan el permiso, esperamos a que vuelva a estar disponible.
            } catch (_: IllegalArgumentException) {
                // Dirección inválida o dispositivo no disponible.
            } finally {
                synchronized(datosLock) {
                    if (socketActivo === candidato) socketActivo = null
                    if (ejecutar) actualizarEstado(EstadoConexion.DESCONECTADO)
                }
                try { candidato?.close() } catch (_: IOException) { }
            }
            esperarReintento()
        }
    }

    /** Las líneas de Arduino deben terminar en salto de línea (Serial.println). */
    private fun leerMensajes(conexion: BluetoothSocket, mac: String) {
        val entrada = conexion.inputStream
        val buffer = ByteArray(1024)
        val pendiente = StringBuilder()
        while (ejecutar && direccionMac == mac && socketActivo === conexion) {
            val cantidad = entrada.read(buffer)
            if (cantidad < 0) throw IOException("Conexión cerrada")
            for (i in 0 until cantidad) {
                val caracter = (buffer[i].toInt() and 0xff).toChar()
                if (caracter == '\n' || caracter == '\r') {
                    val mensaje = pendiente.toString().trim()
                    pendiente.setLength(0)
                    if (mensaje.isNotEmpty()) procesarMensaje(mensaje, conexion, mac)
                } else if (pendiente.length < 4096) {
                    pendiente.append(caracter)
                } else {
                    pendiente.setLength(0) // Evita buffers ilimitados sin delimitadores.
                }
            }
        }
    }

    private fun procesarMensaje(mensaje: String, conexion: BluetoothSocket, mac: String) {
        synchronized(datosLock) {
            if (socketActivo !== conexion || direccionMac != mac) return
            ultimoMensaje = mensaje
            PirMessageParser.evento(mensaje)?.let { evento ->
                when (evento) {
                    PirMessageParser.Evento.ACTIVADO -> {
                        sensorConfirmado = true
                        estadoPir = null // Activado no significa zona despejada.
                        ultimaLecturaPirMs = null
                    }
                    PirMessageParser.Evento.DESACTIVADO -> {
                        sensorConfirmado = false
                        estadoPir = null
                        ultimaLecturaPirMs = null
                    }
                    PirMessageParser.Evento.MOVIMIENTO, PirMessageParser.Evento.SIN_MOVIMIENTO -> {
                        // Ignora una lectura residual después de la orden de apagar,
                        // hasta recibir confirmación de activación o una nueva conexión.
                        if (sensorConfirmado == false || almacenamiento()?.control(Control.SENSOR) == false &&
                            sensorConfirmado == null) return@let
                        sensorConfirmado = true
                        estadoPir = evento == PirMessageParser.Evento.MOVIMIENTO
                        ultimaLecturaPirMs = SystemClock.elapsedRealtime()
                        if (mensaje.trim().uppercase(java.util.Locale.ROOT) in setOf("PIR:0", "PIR:1")) {
                            lecturasPeriodicas = true
                        }
                    }
                }
                if (ultimoEventoPir != evento) {
                    almacenamiento()?.let { datos ->
                        datos.registrarPir(evento, System.currentTimeMillis())
                        ultimaDeteccionMs = datos.ultimaDeteccion()
                    }
                    ultimoEventoPir = evento
                }
            }
        }
        mainHandler.post {
            val copia = synchronized(observadoresLock) { observadoresDatos.toList() }
            copia.forEach { observador ->
                val vigente = synchronized(observadoresLock) { observador in observadoresDatos }
                if (vigente) observador(mensaje)
            }
        }
    }

    private fun encolarComando(conexion: BluetoothSocket, comando: String) {
        escritor.execute {
            if (socketActivo !== conexion) return@execute
            try {
                conexion.outputStream.write(comando.toByteArray(Charsets.US_ASCII))
                conexion.outputStream.flush()
            } catch (_: IOException) {
                synchronized(datosLock) {
                    if (socketActivo === conexion) cerrarSocketActivo()
                }
            } catch (_: SecurityException) {
                synchronized(datosLock) {
                    if (socketActivo === conexion) cerrarSocketActivo()
                }
            }
        }
    }

    private fun cerrarSocketActivo() {
        synchronized(datosLock) {
            val actual = socketActivo
            socketActivo = null
            try { actual?.close() } catch (_: IOException) { }
        }
    }

    private fun esperarReintento() {
        try { Thread.sleep(REINTENTO_MS) } catch (_: InterruptedException) { }
    }

    private fun actualizarEstado(nuevo: EstadoConexion, forzar: Boolean = false) {
        synchronized(datosLock) {
            if (!forzar && estadoActual == nuevo) return
            if (nuevo != EstadoConexion.CONECTADO) {
                estadoPir = null
                sensorConfirmado = null
                ultimoEventoPir = null
                lecturasPeriodicas = false
                ultimoMensaje = null
                ultimaLecturaPirMs = null
            }
            estadoActual = nuevo
        }
        mainHandler.post {
            val copia = synchronized(observadoresLock) { observadoresEstado.toList() }
            copia.forEach { observador ->
                val vigente = synchronized(observadoresLock) { observador in observadoresEstado }
                if (vigente) observador(estadoActual)
            }
        }
    }

    fun agregarListenerEstado(observador: (EstadoConexion) -> Unit) {
        synchronized(observadoresLock) { observadoresEstado.add(observador) }
        mainHandler.post {
            val registrado = synchronized(observadoresLock) { observador in observadoresEstado }
            if (registrado) observador(estadoActual)
        }
    }

    fun quitarListenerEstado(observador: (EstadoConexion) -> Unit) {
        synchronized(observadoresLock) { observadoresEstado.remove(observador) }
    }

    fun agregarListenerDatos(observador: (String) -> Unit) {
        synchronized(observadoresLock) { observadoresDatos.add(observador) }
    }

    fun quitarListenerDatos(observador: (String) -> Unit) {
        synchronized(observadoresLock) { observadoresDatos.remove(observador) }
    }
}
