package com.example.proyectoiot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorActivity : BluetoothBaseActivity() {
    private lateinit var tvEstadoBluetooth: TextView
    private lateinit var tvEstadoPir: TextView
    private lateinit var tvUltimaDeteccion: TextView
    private lateinit var tvMensaje: TextView
    private lateinit var tvHistorial: TextView
    private lateinit var tvResumen: TextView
    private lateinit var btnHistorial: Button
    private var historialMostrado: List<IoTStorage.EventoPir>? = null
    private val handler = Handler(Looper.getMainLooper())
    private val revisarVigencia = object : Runnable {
        override fun run() {
            mostrarEstadoPir()
            handler.postDelayed(this, 1000L)
        }
    }

    private val listenerEstado: (BluetoothManager.EstadoConexion) -> Unit = { estado ->
        tvEstadoBluetooth.text = when (estado) {
            BluetoothManager.EstadoConexion.CONECTADO -> "Bluetooth: conectado"
            BluetoothManager.EstadoConexion.CONECTANDO -> "Bluetooth: conectando..."
            BluetoothManager.EstadoConexion.DESCONECTADO -> "Bluetooth: desconectado, reintentando..."
            BluetoothManager.EstadoConexion.ESPERANDO_BLUETOOTH -> "Bluetooth apagado o sin permiso"
            BluetoothManager.EstadoConexion.SIN_CONFIGURAR -> "Bluetooth: selecciona HC-06"
        }
        tvEstadoBluetooth.setTextColor(
            if (estado == BluetoothManager.EstadoConexion.CONECTADO) Color.rgb(56, 142, 60)
            else Color.rgb(211, 47, 47)
        )
        mostrarDatos()
    }

    private val listenerDatos: (String) -> Unit = {
        mostrarDatos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        tvEstadoBluetooth = findViewById(R.id.tv_estado_bluetooth_sensor)
        tvEstadoPir = findViewById(R.id.valor_estado)
        tvUltimaDeteccion = findViewById(R.id.valor_ultima_deteccion)
        tvMensaje = findViewById(R.id.tv_mensaje_recibido)
        tvHistorial = findViewById(R.id.tv_historial_pir)
        tvResumen = findViewById(R.id.tv_resumen_historial)
        btnHistorial = findViewById(R.id.btn_historial_completo)
        btnHistorial.setOnClickListener { abrirHistorial() }

        findViewById<Button>(R.id.btn_conectar_bluetooth).setOnClickListener { elegirBluetooth() }
        findViewById<Button>(R.id.btn_siguiente).setOnClickListener {
            startActivity(Intent(this, ActuadorActivity::class.java))
        }
        findViewById<Button>(R.id.btn_volver).setOnClickListener {
            finish()
        }
        prepararBluetooth()
    }

    override fun onStart() {
        super.onStart()
        BluetoothManager.agregarListenerEstado(listenerEstado)
        BluetoothManager.agregarListenerDatos(listenerDatos)
        mostrarDatos()
        handler.post(revisarVigencia)
    }

    override fun onStop() {
        handler.removeCallbacks(revisarVigencia)
        BluetoothManager.quitarListenerEstado(listenerEstado)
        BluetoothManager.quitarListenerDatos(listenerDatos)
        super.onStop()
    }

    private fun mostrarDatos() {
        tvMensaje.text = "Último dato: ${BluetoothManager.ultimoMensaje ?: "sin datos"}"
        mostrarEstadoPir()
        val historial = BluetoothManager.historialPir()
        if (historial == historialMostrado) return
        historialMostrado = historial
        val detecciones = historial.count { it.tipo == PirMessageParser.Evento.MOVIMIENTO }
        tvResumen.text = "${historial.size} eventos guardados · $detecciones detecciones"
        btnHistorial.isEnabled = historial.isNotEmpty()
        tvHistorial.text = if (historial.isEmpty()) "Sin eventos registrados" else
            formatearHistorial(historial.asReversed().take(8))
    }

    private fun etiqueta(tipo: PirMessageParser.Evento): String = when (tipo) {
        PirMessageParser.Evento.MOVIMIENTO -> "MOVIMIENTO DETECTADO"
        PirMessageParser.Evento.SIN_MOVIMIENTO -> "SIN MOVIMIENTO"
        PirMessageParser.Evento.ACTIVADO -> "SENSOR ACTIVADO"
        PirMessageParser.Evento.DESACTIVADO -> "SENSOR APAGADO"
    }

    private fun color(tipo: PirMessageParser.Evento): Int = when (tipo) {
        PirMessageParser.Evento.MOVIMIENTO -> Color.rgb(211, 47, 47)
        PirMessageParser.Evento.SIN_MOVIMIENTO -> Color.rgb(56, 142, 60)
        PirMessageParser.Evento.ACTIVADO -> Color.rgb(17, 77, 148)
        PirMessageParser.Evento.DESACTIVADO -> Color.rgb(117, 117, 117)
    }

    private fun formatearHistorial(eventos: List<IoTStorage.EventoPir>): CharSequence {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return SpannableStringBuilder().apply {
            eventos.forEachIndexed { index, evento ->
                if (index > 0) append("\n\n")
                append(formato.format(Date(evento.fechaMs))).append("\n")
                val inicio = length
                append(etiqueta(evento.tipo))
                setSpan(ForegroundColorSpan(color(evento.tipo)), inicio, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun abrirHistorial() {
        val eventos = BluetoothManager.historialPir().asReversed()
        AlertDialog.Builder(this)
            .setTitle("Historial PIR · ${eventos.size} eventos")
            .setMessage(if (eventos.isEmpty()) "Sin eventos registrados" else formatearHistorial(eventos))
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun mostrarEstadoPir() {
        val movimiento = BluetoothManager.estadoPir
        val ultimaLectura = BluetoothManager.ultimaLecturaPirMs
        val conectado = BluetoothManager.estadoActual == BluetoothManager.EstadoConexion.CONECTADO
        val vencido = BluetoothManager.lecturasPeriodicas && ultimaLectura != null &&
            SystemClock.elapsedRealtime() - ultimaLectura >= 10_000L
        val estado = when {
            !conectado -> "SIN CONEXIÓN"
            BluetoothManager.sensorConfirmado == false -> "SENSOR APAGADO"
            BluetoothManager.sensorConfirmado == null &&
                BluetoothManager.estadoControl(BluetoothManager.Control.SENSOR) == false -> "APAGADO (SIN CONFIRMAR)"
            vencido -> "SIN DATOS RECIENTES"
            movimiento == true -> "MOVIMIENTO DETECTADO"
            movimiento == false -> "SIN MOVIMIENTO"
            else -> "ESPERANDO LECTURA PIR"
        }
        tvEstadoPir.text = estado
        tvEstadoPir.setTextColor(when (estado) {
            "MOVIMIENTO DETECTADO" -> Color.rgb(211, 47, 47)
            "SIN MOVIMIENTO" -> Color.rgb(56, 142, 60)
            else -> Color.rgb(117, 117, 117)
        })
        val tiempo = BluetoothManager.ultimaDeteccionMs
        tvUltimaDeteccion.text = tiempo?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(it))
        } ?: "Sin detecciones registradas"
    }
}
