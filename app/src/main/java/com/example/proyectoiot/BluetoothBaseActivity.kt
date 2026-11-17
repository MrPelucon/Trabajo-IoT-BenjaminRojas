package com.example.proyectoiot

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** Permiso y selector de dispositivos vinculados, reutilizado por las dos pantallas. */
abstract class BluetoothBaseActivity : AppCompatActivity() {
    private var elegirDespuesDelPermiso = false

    private val permisoBluetooth = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { autorizado ->
        if (autorizado) continuarBluetooth(elegirDespuesDelPermiso)
        else Toast.makeText(this, "Autoriza Dispositivos cercanos para usar HC-06", Toast.LENGTH_LONG).show()
    }

    /** Llamar una vez desde onCreate. Reutiliza el último HC-06 o pide elegirlo. */
    protected fun prepararBluetooth() = comprobarPermiso(false)

    /** Vincula otro módulo o recupera manualmente uno ya vinculado. */
    protected fun elegirBluetooth() = comprobarPermiso(true)

    override fun onResume() {
        super.onResume()
        // Al regresar de Ajustes de Bluetooth, intenta la MAC anteriormente guardada.
        if (tienePermisoBluetooth()) BluetoothManager.reconectarGuardado(this)
    }

    private fun tienePermisoBluetooth(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    private fun comprobarPermiso(elegir: Boolean) {
        elegirDespuesDelPermiso = elegir
        if (!tienePermisoBluetooth()) {
            permisoBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            continuarBluetooth(elegir)
        }
    }

    @SuppressLint("MissingPermission")
    private fun continuarBluetooth(elegir: Boolean) {
        val adaptador = getSystemService(AndroidBluetoothManager::class.java)?.adapter
        if (adaptador == null) {
            Toast.makeText(this, "El teléfono no tiene Bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        if (!adaptador.isEnabled) {
            AlertDialog.Builder(this)
                .setTitle("Bluetooth apagado")
                .setMessage("Activa Bluetooth en el teléfono y vuelve a la aplicación.")
                .setPositiveButton("Abrir ajustes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        if (!elegir && BluetoothManager.reconectarGuardado(this)) return

        val equipos = BluetoothManager.dispositivosVinculados(this)
        if (equipos.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("HC-06 sin vincular")
                .setMessage("Primero vincula el HC-06 desde los ajustes Bluetooth de Android. Luego regresa y pulsa Conectar HC-06.")
                .setPositiveButton("Abrir ajustes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        val nombres = equipos.map { "${it.name ?: "Dispositivo sin nombre"} (${it.address})" }
            .toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecciona el módulo HC-06")
            .setItems(nombres) { _, indice ->
                BluetoothManager.conectar(this, equipos[indice].address)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
