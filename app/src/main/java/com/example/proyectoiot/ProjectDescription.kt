package com.example.proyectoiot

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.content.Intent

class ProjectDescription : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_project_description)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // -- CONTROL DE NAVEGACION --
        val btnNavAvanzar = findViewById<Button>(R.id.btn_siguiente)
        btnNavAvanzar.setOnClickListener {
            val intent = Intent(this, SensorActivity::class.java)
            startActivity(intent)
        }

        val btnNavRetroceder = findViewById<Button>(R.id.btn_volver)
        btnNavRetroceder.setOnClickListener {
            val intent = Intent(this, LoginView::class.java)
            startActivity(intent)
        }


    }
}