package com.example.proyectoiot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Configuración deseada e historial local, independientes para cada HC-06. */
internal class IoTStorage(context: Context, private val mac: String) {
    data class EventoPir(val fechaMs: Long, val tipo: PirMessageParser.Evento)

    private val prefs = context.getSharedPreferences("iot_estado", Context.MODE_PRIVATE)
    private fun clave(nombre: String) = "$mac:$nombre"

    fun control(control: BluetoothManager.Control): Boolean? {
        val key = clave("control_${control.name}")
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    }

    fun guardarControl(control: BluetoothManager.Control, activo: Boolean) {
        prefs.edit().putBoolean(clave("control_${control.name}"), activo).apply()
    }

    fun ultimaDeteccion(): Long? =
        prefs.getLong(clave("ultima_deteccion"), 0L).takeIf { it > 0L }

    fun historial(): List<EventoPir> {
        val datos = runCatching {
            JSONArray(prefs.getString(clave("historial_pir"), "[]") ?: "[]")
        }.getOrElse { JSONArray() }
        return (0 until datos.length()).mapNotNull { i ->
            val evento = datos.optJSONObject(i) ?: return@mapNotNull null
            if (!evento.has("fechaMs")) return@mapNotNull null
            // Migra el historial anterior de booleanos sin perder las detecciones.
            val tipo = if (evento.has("tipo")) {
                runCatching { PirMessageParser.Evento.valueOf(evento.optString("tipo")) }.getOrNull()
            } else if (evento.has("movimiento")) {
                if (evento.optBoolean("movimiento")) PirMessageParser.Evento.MOVIMIENTO
                else PirMessageParser.Evento.SIN_MOVIMIENTO
            } else null
            tipo?.let { EventoPir(evento.optLong("fechaMs"), it) }
        }
    }

    fun registrarPir(tipo: PirMessageParser.Evento, fechaMs: Long) {
        val anterior = historial()
        // BluetoothManager solo invoca este método para cambios reales observados.
        // No se deduplican sesiones diferentes en disco: podrían ser detecciones distintas.
        val nuevos = (anterior + EventoPir(fechaMs, tipo)).takeLast(MAX_EVENTOS)
        val datos = JSONArray()
        nuevos.forEach { evento ->
            datos.put(JSONObject().put("fechaMs", evento.fechaMs).put("tipo", evento.tipo.name))
        }
        val editor = prefs.edit().putString(clave("historial_pir"), datos.toString())
        if (tipo == PirMessageParser.Evento.MOVIMIENTO) editor.putLong(clave("ultima_deteccion"), fechaMs)
        editor.apply()
    }

    companion object {
        const val MAX_EVENTOS = 200
    }
}
