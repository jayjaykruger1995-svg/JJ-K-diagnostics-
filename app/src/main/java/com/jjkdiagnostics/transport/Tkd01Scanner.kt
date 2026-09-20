package com.jjkdiagnostics.transport

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class Tkd01Scanner(private val context: Context) {
    companion object { const val TARGET = "979869028107" }
    private var leScanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    fun scan(onFound: (BluetoothDevice, String) -> Unit, onStatus: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            onStatus("Bluetooth scan permission is required"); return
        }
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: run { onStatus("Bluetooth unavailable"); return }
        if (!adapter.isEnabled) { onStatus("Switch Bluetooth on"); return }
        leScanner = adapter.bluetoothLeScanner
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        onStatus("Scanning for TKD01 ($TARGET)…")
        leScanner?.startScan(null, settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = result.scanRecord?.deviceName ?: try { device.name } catch (_: SecurityException) { null }
                if (device.address == TARGET || name == TARGET || name?.contains("Think", true) == true) onFound(device, name ?: device.address)
            }
            override fun onScanFailed(errorCode: Int) { onStatus("BLE scan failed: $errorCode") }
        })
    }

    fun stopScan() { try { leScanner?.stopScan(object : ScanCallback() {}) } catch (_: Exception) {} }

    fun connect(device: BluetoothDevice, onStatus: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { onStatus("Bluetooth connect permission is required"); return }
        gatt?.close()
        onStatus("Connecting to TKD01…")
        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothGatt.STATE_CONNECTED) { onStatus("TKD01 connected; discovering services…"); g.discoverServices() }
                else if (newState == BluetoothGatt.STATE_DISCONNECTED) onStatus("TKD01 disconnected ($status)")
            }
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) { onStatus("Service discovery failed: $status"); return }
                var count = 0
                for (service in g.services) for (characteristic in service.characteristics) {
                    val p = characteristic.properties
                    if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) writeCharacteristic = characteristic
                    if ((p and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 || (p and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) notifyCharacteristic = characteristic
                    count++
                }
                onStatus("TKD01 services discovered: $count characteristics")
            }
        })
    }

    fun gatt(): BluetoothGatt? = gatt
    fun writeCharacteristic(): BluetoothGattCharacteristic? = writeCharacteristic
    fun notifyCharacteristic(): BluetoothGattCharacteristic? = notifyCharacteristic
    fun close() { gatt?.close(); gatt = null }
}
