package com.jjkdiagnostics

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.widget.*
import com.jjkdiagnostics.transport.Tkd01Scanner

class MainActivity : Activity() {
    private lateinit var scanner: Tkd01Scanner
    private lateinit var status: TextView
    private lateinit var details: TextView
    private var foundDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = Tkd01Scanner(this)
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,32,32,32) }
        box.addView(TextView(this).apply { text="JJK DIAGNOSTICS"; textSize=28f; setPadding(0,0,0,16) })
        box.addView(TextView(this).apply { text="TKD01 • Generic OBD-II foundation"; textSize=18f })
        status = TextView(this).apply { text="Ready — target: ${Tkd01Scanner.TARGET}"; setPadding(0,24,0,16) }
        box.addView(status)
        details = TextView(this).apply { text="The app will first identify the TKD01 Bluetooth transport before sending vehicle commands."; setPadding(0,8,0,24) }
        box.addView(details)
        box.addView(Button(this).apply { text="FIND TKD01"; setOnClickListener { scan() } })
        box.addView(Button(this).apply { text="CONNECT TO FOUND TKD01"; setOnClickListener { connect() } })
        box.addView(Button(this).apply { text="TRY CLASSIC BLUETOOTH (SPP)"; setOnClickListener { connectClassic() } })
        box.addView(Button(this).apply { text="STOP SCAN"; setOnClickListener { scanner.stopScan(); status.text="Scan stopped" } })
        scroll.addView(box); setContentView(scroll)
        if (Build.VERSION.SDK_INT >= 31) requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 10)
    }

    private fun scan() {
        scanner.scan({ device, name ->
            foundDevice = device
            status.text = "TKD01 found: $name"
            details.text = "Address: ${device.address}\nTap CONNECT TO FOUND TKD01."
        }, { status.text = it })
    }

    private fun connect() {
        val device = foundDevice ?: run { status.text = "Find the TKD01 first"; return }
        scanner.connect(device) { message -> runOnUiThread { status.text = message } }
    }

    private fun connectClassic() {
        val device = foundDevice ?: run { status.text = "Find the TKD01 first"; return }
        scanner.connectClassic(device) { message -> runOnUiThread { status.text = message } }
    }

    override fun onDestroy() { scanner.close(); super.onDestroy() }
}
