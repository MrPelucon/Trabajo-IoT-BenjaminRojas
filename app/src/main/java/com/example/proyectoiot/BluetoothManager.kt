package com.example.proyectoiot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
object BluetoothManager {

    private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var macAddress: String? = null
    @Volatile private var isRunning = false
    @Volatile private var isConnected = false

    private var listener: ((String) -> Unit)? = null
    private var estadoListener: ((Boolean) -> Unit)? = null // LISTENER DE ESTADO
    private val mainHandler = Handler(Looper.getMainLooper())

    fun setListener(nuevoListener: ((String) -> Unit)?) {
        this.listener = nuevoListener
    }

    // Registra el listener del estado (Conectado / Desconectado)
    fun setEstadoListener(nuevoListener: ((Boolean) -> Unit)?) {
        this.estadoListener = nuevoListener
        // Notifica de inmediato el estado actual al abrir la pantalla
        mainHandler.post { nuevoListener?.invoke(isConnected) }
    }

    private fun notificarCambioEstado(conectado: Boolean) {
        if (isConnected != conectado) {
            isConnected = conectado
            mainHandler.post {
                estadoListener?.invoke(conectado)
            }
        }
    }

    fun iniciar(direccionMac: String) {
        this.macAddress = direccionMac
        if (!isRunning) {
            isRunning = true
            hiloReconexion()
        }
    }

    private fun hiloReconexion() {
        Thread {
            while (isRunning) {
                if (!isConnected) {
                    intentarConexion()
                }
                try {
                    Thread.sleep(4000)
                } catch (e: Exception) { }
            }
        }.start()
    }

    private fun intentarConexion() {
        val mac = macAddress ?: return
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return

        try {
            val device = adapter.getRemoteDevice(mac)
            socket = device.createRfcommSocketToServiceRecord(UUID_SPP)
            adapter.cancelDiscovery()
            socket?.connect()

            outputStream = socket?.outputStream
            inputStream = socket?.inputStream

            notificarCambioEstado(true) // CONECTADO CON ÉXITO

            escucharRespuestas()
        } catch (e: Exception) {
            cerrarConexion()
        }
    }

    private fun escucharRespuestas() {
        val buffer = ByteArray(1024)
        while (isConnected && isRunning) {
            try {
                val bytesLeidos = inputStream?.read(buffer) ?: -1
                if (bytesLeidos > 0) {
                    val mensaje = String(buffer, 0, bytesLeidos).trim()
                    mainHandler.post {
                        listener?.invoke(mensaje)
                    }
                } else if (bytesLeidos == -1) {
                    throw Exception("Socket cerrado")
                }
            } catch (e: Exception) {
                cerrarConexion()
                break
            }
        }
    }

    fun enviarComando(comando: String) {
        Thread {
            if (isConnected) {
                try {
                    outputStream?.write(comando.toByteArray())
                } catch (e: Exception) {
                    cerrarConexion()
                }
            }
        }.start()
    }

    private fun cerrarConexion() {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) { }

        socket = null
        outputStream = null
        inputStream = null

        notificarCambioEstado(false) // DESCONECTADO
    }

    fun detener() {
        isRunning = false
        cerrarConexion()
    }
}