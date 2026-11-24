package com.example.proyectoiot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PirMessageParserTest {
    @Test fun reconoceAusenciaDeMovimiento() {
        listOf("0", "PIR:0", "PIR=0", "SENSOR:0", "SIN MOVIMIENTO",
            "No se detectó movimiento.", "Movimiento no detectado",
            "ALERTA: sin movimiento", "PIR:LOW").forEach {
            assertEquals(it, false, PirMessageParser.movimiento(it))
        }
    }

    @Test fun reconoceMovimiento() {
        listOf("1", "PIR:1", "PIR=1", "SENSOR:1", "MOVIMIENTO DETECTADO",
            "ALERTA: ¡Movimiento detectado!", "PIR:HIGH").forEach {
            assertEquals(it, true, PirMessageParser.movimiento(it))
        }
    }

    @Test fun ignoraMensajesNoReconocidos() {
        listOf("", "LED1:1", "SENSOR DESACTIVADO", "PIR:OFF",
            "ALERTA: batería baja", "NO HAY ALERTA DE MOVIMIENTO").forEach {
            assertNull(it, PirMessageParser.movimiento(it))
        }
    }
}
