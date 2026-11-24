package com.example.proyectoiot

import java.text.Normalizer
import java.util.Locale

/** Interpreta estados explícitos del PIR enviados como líneas CR/LF. */
object PirMessageParser {
    enum class Evento { MOVIMIENTO, SIN_MOVIMIENTO, ACTIVADO, DESACTIVADO }

    fun movimiento(mensaje: String): Boolean? = when (evento(mensaje)) {
        Evento.MOVIMIENTO -> true
        Evento.SIN_MOVIMIENTO -> false
        else -> null
    }

    fun evento(mensaje: String): Evento? {
        val dato = Normalizer.normalize(mensaje, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9:=]"), "")
        return when (dato) {
            "OK:SENSORPIRACTIVADO", "SENSORPIRACTIVADO", "PIR:ON", "PIR=ON" -> Evento.ACTIVADO
            "OK:SENSORPIRDESACTIVADO", "SENSORPIRDESACTIVADO", "PIR:OFF", "PIR=OFF" -> Evento.DESACTIVADO
            "ESTADO:ZONADESPEJADA", "NADA:ZONADESPEJADA", "ZONADESPEJADA",
            "ESTADO:SINMOVIMIENTO", "ESTADO:NOMOVIMIENTO" -> Evento.SIN_MOVIMIENTO
            "0", "PIR:0", "PIR=0", "SENSOR:0", "SENSOR=0",
            "SINMOVIMIENTO", "NOMOVIMIENTO", "NOHAYMOVIMIENTO",
            "MOVIMIENTONODETECTADO", "NOSEDETECTAMOVIMIENTO",
            "NOSEDETECTOMOVIMIENTO", "SINMOVIMIENTODETECTADO",
            "NOSEHADETECTADOMOVIMIENTO", "PIR:LOW", "PIR=LOW",
            "PIR:SINMOVIMIENTO", "PIR=SINMOVIMIENTO",
            "MOVIMIENTO:0", "MOVIMIENTO=0", "SINMOVIMIENTOENELAREA",
            "ALERTA:SINMOVIMIENTO", "ALERTA:NOMOVIMIENTO",
            "ALERTA:MOVIMIENTONODETECTADO", "NOSEDETECTAMOVIMIENTOENELAREA" -> Evento.SIN_MOVIMIENTO
            "1", "PIR:1", "PIR=1", "SENSOR:1", "SENSOR=1",
            "MOVIMIENTO", "MOVIMIENTODETECTADO", "SEDETECTOMOVIMIENTO",
            "SEDETECTAMOVIMIENTO", "ALERTA", "ALERTA:MOVIMIENTODETECTADO",
            "ALERTA:SEDETECTOMOVIMIENTO", "ALERTAMOVIMIENTODETECTADO",
            "PIR:HIGH", "PIR=HIGH", "PIR:MOVIMIENTO", "PIR=MOVIMIENTO",
            "MOVIMIENTO:1", "MOVIMIENTO=1", "MOVIMIENTODETECTADOENELAREA" -> Evento.MOVIMIENTO
            else -> null
        }
    }
}
