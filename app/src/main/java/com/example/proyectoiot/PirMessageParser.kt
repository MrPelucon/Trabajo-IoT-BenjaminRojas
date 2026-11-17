package com.example.proyectoiot

import java.util.Locale

/** Interpreta mensajes de Arduino terminados en \n (Serial.println). */
object PirMessageParser {
    fun movimiento(mensaje: String): Boolean? {
        val dato = mensaje.trim().uppercase(Locale.ROOT).replace(" ", "")
        return when {
            dato == "0" || dato == "PIR:0" || dato == "PIR=0" ||
                dato == "SENSOR:0" || dato == "SINMOVIMIENTO" ||
                dato == "NOMOVIMIENTO" -> false
            dato == "1" || dato == "PIR:1" || dato == "PIR=1" ||
                dato == "SENSOR:1" || dato == "MOVIMIENTO" ||
                dato.contains("MOVIMIENTODETECTADO") || dato.contains("ALERTA") -> true
            else -> null
        }
    }
}
