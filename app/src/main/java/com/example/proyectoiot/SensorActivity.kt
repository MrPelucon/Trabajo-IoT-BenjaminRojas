package com.example.proyectoiot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
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
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        tvHistorial.text = if (historial.isEmpty()) "Sin eventos registrados" else
            historial.asReversed().joinToString("\n") { evento ->
                val estado = if (evento.movimiento) "MOVIMIENTO DETECTADO" else "SIN MOVIMIENTO"
                "${formato.format(Date(evento.fechaMs))}  ·  $estado"
            }
    }

    private fun mostrarEstadoPir() {
        val movimiento = BluetoothManager.estadoPir
        val ultimaLectura = BluetoothManager.ultimaLecturaPirMs
        val conectado = BluetoothManager.estadoActual == BluetoothManager.EstadoConexion.CONECTADO
        val vencido = ultimaLectura != null && SystemClock.elapsedRealtime() - ultimaLectura >= 10_000L
        val estado = when {
            !conectado -> "SIN CONEXIÓN"
            vencido -> "SIN DATOS RECIENTES"
            movimiento == true -> "MOVIMIENTO DETECTADO"
            movimiento == false -> "SIN MOVIMIENTO"
            BluetoothManager.estadoControl(BluetoothManager.Control.SENSOR) == false -> "SENSOR APAGADO (CONFIG.)"
            else -> "ESPERANDO DATOS"
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
