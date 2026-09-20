package com.jjkdiagnostics.transport
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

class Tkd01Scanner(private val context: Context) {
    companion object { const val TARGET = "979869028107" }
    private var leScanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var scanCallback: ScanCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectAttempt = 0
    private var classicSocket: android.bluetooth.BluetoothSocket? = null

    fun scan(onFound: (BluetoothDevice, String) -> Unit, onStatus: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) { onStatus("Bluetooth scan permission is required"); return }
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: run { onStatus("Bluetooth unavailable"); return }
        if (!adapter.isEnabled) { onStatus("Switch Bluetooth on"); return }
        leScanner = adapter.bluetoothLeScanner
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        onStatus("Scanning for TKD01 ($TARGET)…")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = result.scanRecord?.deviceName ?: try { device.name } catch (_: SecurityException) { null }
                if (name == TARGET || name?.contains("Think", true) == true) {
                    try { leScanner?.stopScan(this) } catch (_: Exception) {}
                    onFound(device, name ?: device.address)
                }
            }
            override fun onScanFailed(errorCode: Int) { onStatus("BLE scan failed: $errorCode") }
        }
        scanCallback = callback
        leScanner?.startScan(null, settings, callback)
    }

    fun stopScan() {
        try { scanCallback?.let { leScanner?.stopScan(it) } } catch (_: Exception) {}
        scanCallback = null
    }

    fun connect(device: BluetoothDevice, onStatus: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { onStatus("Bluetooth connect permission is required"); return }
        stopScan()
        gatt?.close()
        writeCharacteristic = null
        notifyCharacteristic = null
        connectAttempt = 1
        connectInternal(device, onStatus)
    }

    private fun connectInternal(device: BluetoothDevice, onStatus: (String) -> Unit) {
        onStatus("Connecting to TKD01…\nAttempt=$connectAttempt Type=${device.type} Bond=${device.bondState}")
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    onStatus("TKD01 connected; discovering services…")
                    g.discoverServices()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    onStatus("TKD01 disconnected (GATT status=$status, attempt=$connectAttempt)")
                    if (connectAttempt < 3) {
                        connectAttempt++
                        try { g.close() } catch (_: Exception) {}
                        mainHandler.postDelayed({ connectInternal(device, onStatus) }, 1200)
                    }
                }
            }
            override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onStatus("Notifications established — TKD01 BLE channel is open.")
                } else {
                    onStatus("Notification setup failed: GATT status=$status")
                }
            }

            override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                val hex = value.joinToString(" ") { b -> "%02X".format(b) }
                onStatus("READ " + characteristic.uuid + ": status=" + status + " bytes=" + value.size + " data=" + hex)
            }

            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                val hex = value.joinToString(" ") { b -> "%02X".format(b) }
                val ascii = value.map { b -> if (b.toInt() in 32..126) b.toInt().toChar() else '.' }.joinToString("")
                onStatus("NOTIFY " + characteristic.uuid + ": " + value.size + " bytes | " + hex + " | ASCII=" + ascii)
            }

            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                onStatus("MTU result: mtu=" + mtu + " status=" + status)
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) { onStatus("Service discovery failed: $status"); return }
                var count = 0
                for (service in g.services) {
                    onStatus("Service: $service.uuid")
                    for (characteristic in service.characteristics) {
                        val p = characteristic.properties
                        if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) writeCharacteristic = characteristic
                        if ((p and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 || (p and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) notifyCharacteristic = characteristic
                        onStatus("Characteristic: $characteristic.uuid props=$p")
                        count++
                    }
                }
                onStatus("GATT map complete: $count characteristics; write=${writeCharacteristic != null}, notify=${notifyCharacteristic != null}")
                val notify = notifyCharacteristic
                if (notify != null) {
                    try {
                        g.setCharacteristicNotification(notify, true)
                        val descriptor = notify.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (descriptor != null) {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            g.writeDescriptor(descriptor)
                            onStatus("Notifications establishing…")
                            mainHandler.postDelayed({
                                if (gatt === g && notifyCharacteristic === notify) {
                                    onStatus("Notification setup timed out — TKD01 accepted the connection but did not confirm the notification channel.")
                                }
                            }, 5000)
                        } else {
                            onStatus("No CCCD found; connection remains open")
                        }
                    } catch (e: Exception) {
                        onStatus("Notification setup error: \${e.message}")
                    }
                } else {
                    onStatus("No notify characteristic; connection remains open")
                }
            }
        }
        gatt = if (Build.VERSION.SDK_INT >= 23) device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE) else device.connectGatt(context, false, callback)
    }

    fun connectClassic(device: BluetoothDevice, onStatus: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            onStatus("Bluetooth connect permission is required"); return
        }
        stopScan()
        onStatus("Trying TKD01 Classic Bluetooth (SPP)…")
        Thread {
            var socket: android.bluetooth.BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket.connect()
                classicSocket = socket
                onStatus("TKD01 Classic Bluetooth connected — SPP channel is open.")
                val input = socket.inputStream
                val output = socket.outputStream
                output.write("\r".toByteArray())
                output.flush()
                val buffer = ByteArray(256)
                val read = input.read(buffer)
                val response = if (read > 0) String(buffer, 0, read).replace("\r", " ").replace("\n", " ").trim() else "(no response)"
                onStatus("TKD01 Classic test response: $response")
            } catch (e: Exception) {
                onStatus("TKD01 Classic Bluetooth failed: " + e.javaClass.simpleName + ": " + (e.message ?: "unknown error"))
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    fun gatt(): BluetoothGatt? = gatt
    fun writeCharacteristic(): BluetoothGattCharacteristic? = writeCharacteristic
    fun notifyCharacteristic(): BluetoothGattCharacteristic? = notifyCharacteristic
    fun close() { gatt?.close(); gatt = null }
}
