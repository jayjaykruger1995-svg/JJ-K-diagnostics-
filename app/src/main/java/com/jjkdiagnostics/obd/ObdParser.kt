package com.jjkdiagnostics.obd

object ObdParser {
    fun clean(raw: String): String = raw.uppercase().replace("SEARCHING...", "").replace("SEARCHING...", "").replace(">", " ").trim()

    fun bytes(raw: String): IntArray {
        val cleaned = clean(raw).replace("\\r", " ").replace("\\n", " ")
        return cleaned.split(Regex("\\s+"))
            .filter { it.matches(Regex("[0-9A-F]{2}")) }
            .map { it.toInt(16) }.toIntArray()
    }

    fun findMode01Pid(raw: String, pid: Int): IntArray? {
        val b = bytes(raw)
        for (i in 0 until b.size - 2) {
            if (b[i] == 0x41 && b[i + 1] == pid) return b.copyOfRange(i + 2, b.size)
        }
        return null
    }

    fun findDtc(raw: String): List<String> {
        val b = bytes(raw)
        val out = mutableListOf<String>()
        var i = 0
        while (i + 1 < b.size) {
            if (b[i] == 0x43 || b[i] == 0x47) { i++; continue }
            val value = (b[i] shl 8) or b[i + 1]
            if (value == 0) { i += 2; continue }
            val first = when ((b[i] shr 6) and 3) { 0 -> 'P'; 1 -> 'C'; 2 -> 'B'; else -> 'U' }
            val code = "${first}${(b[i] shr 4) and 3}${b[i] and 15}${(b[i + 1] shr 4) and 15}${b[i + 1] and 15}"
            out += code
            i += 2
        }
        return out.distinct()
    }
}
