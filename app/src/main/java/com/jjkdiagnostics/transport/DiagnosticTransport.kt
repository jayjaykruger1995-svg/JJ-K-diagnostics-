package com.jjkdiagnostics.transport

interface DiagnosticTransport {
    val description: String
    val connected: Boolean
    fun connect(onResult: (Boolean, String) -> Unit)
    fun send(command: String, onResult: (String) -> Unit)
    fun close()
}
