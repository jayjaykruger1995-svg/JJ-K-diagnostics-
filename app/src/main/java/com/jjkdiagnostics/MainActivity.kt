package com.jjkdiagnostics

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    private val target = "979869028107"
    private var status: TextView? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val d = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (d != null && (d.name == target || d.address == target)) status?.text = "TKD01 found: ${d.name ?: d.address}"
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,32,32,32) }
        box.addView(TextView(this).apply { text="JJK DIAGNOSTICS"; textSize=28f; setPadding(0,0,0,16) })
        box.addView(TextView(this).apply { text="TKD01 • Bluetooth / Generic OBD-II"; textSize=18f })
        status = TextView(this).apply { text="Ready — target: $target"; setPadding(0,24,0,24) }
        box.addView(status)
        box.addView(Button(this).apply { text="FIND TKD01"; setOnClickListener { findTkd() } })
        box.addView(Button(this).apply { text="CONNECT / TEST TKD01"; setOnClickListener { status?.text="TKD01 transport test will be added next." } })
        setContentView(box)
        if (android.os.Build.VERSION.SDK_INT >= 31) requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 10)
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }
    private fun findTkd() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: run { status?.text="Bluetooth not available"; return }
        if (!adapter.isEnabled) { status?.text="Please switch Bluetooth on"; return }
        status?.text="Scanning for $target…"
        adapter.cancelDiscovery()
        adapter.startDiscovery()
    }
    override fun onDestroy() { unregisterReceiver(receiver); super.onDestroy() }
}
