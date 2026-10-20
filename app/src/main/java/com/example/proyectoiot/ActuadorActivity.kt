package com.example.proyectoiot // Asegúrate de que este sea tu paquete

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class ActuadorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actuador)

        // 1. Vinculamos los elementos
        val switchSensor = findViewById<Switch>(R.id.switch_sensor)
        val switchLed1 = findViewById<Switch>(R.id.switch_led1)
        val switchLed2 = findViewById<Switch>(R.id.switch_led2)
        val btnVolver = findViewById<Button>(R.id.btn_volver)

        // 2. Función genérica para cambiar el aspecto visual de cualquier Switch
        fun actualizarAspectoSwitch(miSwitch: Switch, estaEncendido: Boolean) {
            if (estaEncendido) {
                miSwitch.text = "Encendido  "
                miSwitch.setTextColor(Color.parseColor("#388E3C")) // Texto color Verde

                // Cambiar el color del interruptor a Verde
                miSwitch.thumbTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                miSwitch.trackTintList = ColorStateList.valueOf(Color.parseColor("#A5D6A7"))
            } else {
                miSwitch.text = "Apagado  "
                miSwitch.setTextColor(Color.parseColor("#757575")) // Texto color Gris

                // Cambiar el color del interruptor a Gris/Rojo apagado
                miSwitch.thumbTintList = ColorStateList.valueOf(Color.parseColor("#F1F1F1"))
                miSwitch.trackTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
            }
        }

        // 3. Aplicamos los eventos a cada Switch
        switchLed1.setOnCheckedChangeListener { buttonView, isChecked ->
            actualizarAspectoSwitch(buttonView as Switch, isChecked)

            // LED 1 funciones Apagado y encencido
            if (isChecked) {
                // enviarDatoArduino("A")
            } else {
                // enviarDatoArduino("B")
            }
        }

        switchLed2.setOnCheckedChangeListener { buttonView, isChecked ->
            actualizarAspectoSwitch(buttonView as Switch, isChecked)

            // LED 2 funciones apagado y encendido
        }

        switchSensor.setOnCheckedChangeListener { buttonView, isChecked ->
            actualizarAspectoSwitch(buttonView as Switch, isChecked)

            // SENSOR apagar y encenderlo
        }

        // Configuración inicial para que tomen el color gris al abrir la pantalla
        actualizarAspectoSwitch(switchLed1, false)
        actualizarAspectoSwitch(switchLed2, false)
        actualizarAspectoSwitch(switchSensor, false)

        // Evento volver
        btnVolver.setOnClickListener {
            finish()
        }
    }
}