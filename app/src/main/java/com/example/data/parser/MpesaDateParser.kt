package com.example.data.parser

import java.text.SimpleDateFormat
import java.util.Locale

object MpesaDateParser {
    private val dateFormats = listOf(
        "d/M/yy h:mm a",
        "dd/MM/yy h:mm a",
        "d/M/yyyy h:mm a",
        "dd/MM/yyyy h:mm a",
        "M/d/yy h:mm a",
        "yyyy-MM-dd HH:mm:ss",
        "d/M/yy HH:mm"
    )

    fun parseTimestamp(dateStr: String, timeStr: String, fallbackTime: Long): Long {
        val cleanDate = dateStr.trim().replace(".", "")
        val cleanTime = timeStr.trim().replace(".", "").uppercase(Locale.US)
        if (cleanDate.isEmpty() || cleanTime.isEmpty()) return fallbackTime
        val combined = "$cleanDate $cleanTime"

        for (pattern in dateFormats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = true
                }
                val date = format.parse(combined)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
            }
        }
        return fallbackTime
    }
}
