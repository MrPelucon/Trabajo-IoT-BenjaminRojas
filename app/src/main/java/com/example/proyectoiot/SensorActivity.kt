package com.example.proyectoiot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        // -- CONTROL DE NAVEGACION --
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
}