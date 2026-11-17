package com.example.proyectoiot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SensorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // -- CONTROL DE NAVEGACIÓN --
        val btnNavAvanzar = findViewById<Button>(R.id.btn_siguiente)
        btnNavAvanzar.setOnClickListener {
            val intent = Intent(this, ActuadorActivity::class.java)
            startActivity(intent)
        }

        val btnNavRetroceder = findViewById<Button>(R.id.btn_volver)
        btnNavRetroceder.setOnClickListener {
            val intent = Intent(this, ProjectDescription::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Escucha los mensajes que provienen del Arduino mientras esta pantalla esté activa
        BluetoothManager.setListener { mensaje ->
            if (mensaje.contains("ALERTA")) {
                Toast.makeText(this, "⚠️ $mensaje", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Remueve la escucha de esta pantalla al cambiar a otra ventana
        BluetoothManager.setListener(null)
    }
}