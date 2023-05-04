package com.example.satellitechat.utilities.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.satellitechat.activity.client.chat.OutGoingCallActivity
import com.example.satellitechat.utilities.constants.Constants

class DeclineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.action) {
            Constants.ACTION_DECLINE -> {
                val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(Constants.NOTIFICATION_ID)
            }
        }
    }
}
