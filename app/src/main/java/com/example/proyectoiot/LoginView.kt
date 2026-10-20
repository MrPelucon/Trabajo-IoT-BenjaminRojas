package com.example.proyectoiot

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.content.Intent
import android.widget.EditText
import android.widget.TextView
import android.view.View
import android.util.Log

class LoginView : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencia al TextView de error
        val txtErrorLogin = findViewById<TextView>(R.id.txt_error_login)

        // VALORES REGEX PARA VALIDACION DE EMAIL CON DOMINIO '@INACAPMAIL.CL'
        val emailRegex = """[a-z0-9.]+@inacapmail.cl""".toRegex()

        // -- Logica Botones de Navegacion --
        val btnValidarLogin = findViewById<Button>(R.id.btn_validacion_login)
        btnValidarLogin.setOnClickListener {
            val campoCorreo = findViewById<EditText>(R.id.campo_email)
            val textoCampoCorreo = campoCorreo.text.toString()
            Log.d("Login", "Correo Ingresado: $textoCampoCorreo")

            val campoPassword = findViewById<EditText>(R.id.campo_password)
            val textoCampoPassword = campoPassword.text.toString()
            Log.d("Login", "Contraseña: $textoCampoPassword")

            // VALIDACION DE CREDENCIALES
            if(emailRegex.matches(textoCampoCorreo.lowercase().trim()) && textoCampoPassword == ""){
                txtErrorLogin.visibility = View.GONE
                val intent = Intent(this, ProjectDescription::class.java)
                startActivity(intent)
            } else {
                // Mostrar el mensaje en rojo si falla la validación
                txtErrorLogin.visibility = View.VISIBLE
            }
        }

        val btnNavRetroceder = findViewById<Button>(R.id.btn_volver)
        btnNavRetroceder.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}