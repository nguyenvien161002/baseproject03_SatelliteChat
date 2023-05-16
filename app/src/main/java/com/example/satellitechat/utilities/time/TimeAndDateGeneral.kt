package com.example.satellitechat.utilities.time

import java.text.SimpleDateFormat
import java.util.HashMap

class TimeAndDateGeneral {

    fun getCurrentTimeAndDate(): HashMap<String, Any> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val timeFormat = SimpleDateFormat("hh:mm:ss a")
        val currentDate: String = dateFormat.format(System.currentTimeMillis())
        val currentTime: String = timeFormat.format(System.currentTimeMillis())
        val messageState: HashMap<String, Any> = HashMap()
        messageState["date"] = currentDate
        messageState["time"] = currentTime
        return messageState
    }

    fun timeFormatDurationCall(durationInMillis: Long): String {
        val durationInSeconds = durationInMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        val stringFormat = if (hours >= 1) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        return stringFormat
    }
}