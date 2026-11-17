package com.example.proyectoiot

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class ActuadorActivity : BluetoothBaseActivity() {
    private lateinit var tvEstadoBluetooth: TextView
    private lateinit var switchSensor: Switch
    private lateinit var switchLed1: Switch
    private lateinit var switchLed2: Switch

    private val listenerEstado: (BluetoothManager.EstadoConexion) -> Unit = { estado ->
        val conectado = estado == BluetoothManager.EstadoConexion.CONECTADO
        tvEstadoBluetooth.text = when (estado) {
            BluetoothManager.EstadoConexion.CONECTADO -> "Bluetooth: conectado"
            BluetoothManager.EstadoConexion.CONECTANDO -> "Bluetooth: conectando..."
            BluetoothManager.EstadoConexion.DESCONECTADO -> "Bluetooth: desconectado, reintentando..."
            BluetoothManager.EstadoConexion.ESPERANDO_BLUETOOTH -> "Bluetooth apagado o sin permiso"
            BluetoothManager.EstadoConexion.SIN_CONFIGURAR -> "Bluetooth: selecciona HC-06"
        }
        tvEstadoBluetooth.setTextColor(
            if (conectado) Color.rgb(56, 142, 60) else Color.rgb(211, 47, 47)
        )
        // Evita que la interfaz acepte comandos que se perderían sin conexión.
        switchSensor.isEnabled = conectado
        switchLed1.isEnabled = conectado
        switchLed2.isEnabled = conectado
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actuador)

        tvEstadoBluetooth = findViewById(R.id.tv_estado_bluetooth)
        switchSensor = findViewById(R.id.switch_sensor)
        switchLed1 = findViewById(R.id.switch_led1)
        switchLed2 = findViewById(R.id.switch_led2)

        switchLed1.setOnCheckedChangeListener { _, activo ->
            actualizarAspecto(switchLed1, activo)
            mandar(if (activo) "A" else "B")
        }
        switchLed2.setOnCheckedChangeListener { _, activo ->
            actualizarAspecto(switchLed2, activo)
            mandar(if (activo) "C" else "D")
        }
        switchSensor.setOnCheckedChangeListener { _, activo ->
            actualizarAspecto(switchSensor, activo)
            mandar(if (activo) "E" else "F")
        }

        actualizarAspecto(switchLed1, switchLed1.isChecked)
        actualizarAspecto(switchLed2, switchLed2.isChecked)
        actualizarAspecto(switchSensor, switchSensor.isChecked)
        switchSensor.isEnabled = false
        switchLed1.isEnabled = false
        switchLed2.isEnabled = false

        findViewById<Button>(R.id.btn_conectar_bluetooth).setOnClickListener { elegirBluetooth() }
        findViewById<Button>(R.id.btn_volver).setOnClickListener { finish() }
        prepararBluetooth()
    }

    private fun mandar(comando: String) {
        if (!BluetoothManager.enviarComando(comando)) {
            Toast.makeText(this, "Sin conexión Bluetooth. Comando no enviado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarAspecto(control: Switch, activo: Boolean) {
        control.text = if (activo) "Encendido" else "Apagado"
        control.setTextColor(Color.parseColor(if (activo) "#388E3C" else "#757575"))
        control.thumbTintList = ColorStateList.valueOf(
            Color.parseColor(if (activo) "#4CAF50" else "#F1F1F1")
        )
        control.trackTintList = ColorStateList.valueOf(
            Color.parseColor(if (activo) "#A5D6A7" else "#BDBDBD")
        )
    }

    override fun onStart() {
        super.onStart()
        BluetoothManager.agregarListenerEstado(listenerEstado)
    }

    override fun onStop() {
        BluetoothManager.quitarListenerEstado(listenerEstado)
        super.onStop()
    }
}
