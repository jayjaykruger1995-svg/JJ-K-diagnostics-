package com.jjkdiagnostics.obd

data class ObdPid(val mode: Int, val pid: Int, val name: String, val unit: String, val decoder: (IntArray) -> String)

object StandardPids {
    val supported = listOf(
        ObdPid(1, 0x0C, "Engine RPM", "rpm") { b -> (((b[0] * 256) + b[1]) / 4).toString() },
        ObdPid(1, 0x0D, "Vehicle Speed", "km/h") { b -> b[0].toString() },
        ObdPid(1, 0x05, "Coolant Temperature", "°C") { b -> (b[0] - 40).toString() },
        ObdPid(1, 0x11, "Throttle Position", "%") { b -> "%.1f".format(b[0] * 100.0 / 255.0) },
        ObdPid(1, 0x0F, "Intake Air Temperature", "°C") { b -> (b[0] - 40).toString() },
        ObdPid(1, 0x10, "MAF", "g/s") { b -> "%.2f".format(((b[0] * 256) + b[1]) / 100.0) },
        ObdPid(1, 0x2F, "Fuel Level", "%") { b -> "%.1f".format(b[0] * 100.0 / 255.0) }
    )
}
