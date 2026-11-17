package com.example.proyectoiot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
    }

    private val listenerDatos: (String) -> Unit = { mensaje ->
        tvMensaje.text = "Último dato: $mensaje"
        mostrarEstadoPir()
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
        tvMensaje.text = "Último dato: ${BluetoothManager.ultimoMensaje ?: "sin datos"}"
        mostrarEstadoPir()
    }

    override fun onStop() {
        BluetoothManager.quitarListenerEstado(listenerEstado)
        BluetoothManager.quitarListenerDatos(listenerDatos)
        super.onStop()
    }

    private fun mostrarEstadoPir() {
        val movimiento = BluetoothManager.estadoPir
        tvEstadoPir.text = when (movimiento) {
            true -> "MOVIMIENTO DETECTADO"
            false -> "SIN MOVIMIENTO"
            null -> "ESPERANDO DATOS"
        }
        tvEstadoPir.setTextColor(
            if (movimiento == true) Color.rgb(211, 47, 47) else Color.rgb(56, 142, 60)
        )
        val tiempo = BluetoothManager.ultimaDeteccionMs
        tvUltimaDeteccion.text = tiempo?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(it))
        } ?: "Sin detecciones registradas"
    }
}
