package com.example.satellitechat.utilities.time

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.HashMap

class CurrentTimeAndDate {
    @SuppressLint("SimpleDateFormat")
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
}