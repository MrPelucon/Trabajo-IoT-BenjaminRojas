package com.example.proyectoiot

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ActuadorActivity : AppCompatActivity() {

    private lateinit var tvEstadoBluetooth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actuador)

        // Iniciar servicio de reconexión y comunicación (Reemplazar por tu dirección MAC real)
        BluetoothManager.iniciar("00:21:11:01:86:57")

        // 1. Vinculamos los elementos de la vista
        tvEstadoBluetooth = findViewById(R.id.tv_estado_bluetooth)
        val switchSensor = findViewById<Switch>(R.id.switch_sensor)
        val switchLed1 = findViewById<Switch>(R.id.switch_led1)
        val switchLed2 = findViewById<Switch>(R.id.switch_led2)
        val btnVolver = findViewById<Button>(R.id.btn_volver)

        // 2. Función genérica para cambiar el aspecto visual de cualquier Switch
        fun actualizarAspectoSwitch(miSwitch: Switch, estaEncendido: Boolean) {
            if (estaEncendido) {
                miSwitch.text = "Encendido"
                miSwitch.setTextColor(Color.parseColor("#388E3C")) // Texto color Verde

                // Cambiar el color del interruptor a Verde
                miSwitch.thumbTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                miSwitch.trackTintList = ColorStateList.valueOf(Color.parseColor("#A5D6A7"))
            } else {
                miSwitch.text = "Apagado"
                miSwitch.setTextColor(Color.parseColor("#757575")) // Texto color Gris

                // Cambiar el color del interruptor a Gris
                miSwitch.thumbTintList = ColorStateList.valueOf(Color.parseColor("#F1F1F1"))
                miSwitch.trackTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
            }
        }

        // 3. Aplicamos los eventos a cada Switch y enviamos comandos a Arduino

        // LED 1 (Rojo): 'A' para ON, 'B' para OFF
        switchLed1.setOnCheckedChangeListener { buttonView, isChecked ->
            actualizarAspectoSwitch(buttonView as Switch, isChecked)
            if (isChecked) {
                BluetoothManager.enviarComando("A")
            } else {
                BluetoothManager.enviarComando("B")
            }
        }

        // LED 2 (Verde): 'C' para ON, 'D' para OFF
        switchLed2.setOnCheckedChangeListener { buttonView, isChecked ->
            actualizarAspectoSwitch(buttonView as Switch, isChecked)
            if (isChecked) {
                BluetoothManager.enviarComando("C")
            } else {
                BluetoothManager.enviarComando("D")
            }
        }

        // Sensor PIR: 'E' para Activar, 'F' para Desactivar
        switchSensor.setOnCheckedChangeListener { buttonView, isChecked ->
            actualizarAspectoSwitch(buttonView as Switch, isChecked)
            if (isChecked) {
                BluetoothManager.enviarComando("E")
            } else {
                BluetoothManager.enviarComando("F")
            }
        }

        // Configuración inicial visual (apagados por defecto)
        actualizarAspectoSwitch(switchLed1, false)
        actualizarAspectoSwitch(switchLed2, false)
        actualizarAspectoSwitch(switchSensor, false)

        // Evento volver
        btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Registrar listener para actualizar el TextView de estado en tiempo real
        BluetoothManager.setEstadoListener { conectado ->
            if (conectado) {
                tvEstadoBluetooth.text = "● Conectado"
                tvEstadoBluetooth.setTextColor(Color.parseColor("#4CAF50")) // Verde
            } else {
                tvEstadoBluetooth.text = "● Desconectado (Reconectando...)"
                tvEstadoBluetooth.setTextColor(Color.parseColor("#F44336")) // Rojo
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Liberar el listener al salir de esta pantalla
        BluetoothManager.setEstadoListener(null)
    }
}