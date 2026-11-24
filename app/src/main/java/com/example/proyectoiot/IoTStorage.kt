package com.example.proyectoiot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Configuración deseada e historial local, independientes para cada HC-06. */
internal class IoTStorage(context: Context, private val mac: String) {
    data class EventoPir(val fechaMs: Long, val movimiento: Boolean)

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
            if (!evento.has("fechaMs") || !evento.has("movimiento")) return@mapNotNull null
            EventoPir(evento.optLong("fechaMs"), evento.optBoolean("movimiento"))
        }
    }

    fun registrarPir(movimiento: Boolean, fechaMs: Long) {
        val anterior = historial()
        if (anterior.lastOrNull()?.movimiento == movimiento) return
        val nuevos = (anterior + EventoPir(fechaMs, movimiento)).takeLast(MAX_EVENTOS)
        val datos = JSONArray()
        nuevos.forEach { evento ->
            datos.put(JSONObject().put("fechaMs", evento.fechaMs).put("movimiento", evento.movimiento))
        }
        val editor = prefs.edit().putString(clave("historial_pir"), datos.toString())
        if (movimiento) editor.putLong(clave("ultima_deteccion"), fechaMs)
        editor.apply()
    }

    companion object {
        const val MAX_EVENTOS = 200
    }
}
