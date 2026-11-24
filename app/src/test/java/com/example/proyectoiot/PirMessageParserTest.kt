package com.example.proyectoiot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PirMessageParserTest {
    @Test fun interpretaMensajesDelArduinoOriginal() {
        assertEquals(PirMessageParser.Evento.MOVIMIENTO, PirMessageParser.evento("ALERTA: Movimiento detectado"))
        assertEquals(PirMessageParser.Evento.SIN_MOVIMIENTO, PirMessageParser.evento("ESTADO: Zona despejada"))
        assertEquals(PirMessageParser.Evento.SIN_MOVIMIENTO, PirMessageParser.evento("NADA: Zona despejada"))
        assertEquals(PirMessageParser.Evento.ACTIVADO, PirMessageParser.evento("OK: Sensor PIR ACTIVADO"))
        assertEquals(PirMessageParser.Evento.DESACTIVADO, PirMessageParser.evento("OK: Sensor PIR DESACTIVADO"))
    }

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

    @Test fun noConfundeApagadoConAusencia() {
        assertNull(PirMessageParser.movimiento("PIR:OFF"))
        assertEquals(PirMessageParser.Evento.DESACTIVADO, PirMessageParser.evento("PIR:OFF"))
        assertNull(PirMessageParser.movimiento("OK: Sensor PIR ACTIVADO"))
        listOf("", "LED1:1", "OK: LED Rojo ENCENDIDO", "ALERTA: batería baja").forEach {
            assertNull(it, PirMessageParser.evento(it))
        }
    }
}
