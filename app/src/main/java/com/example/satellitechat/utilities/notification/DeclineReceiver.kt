package com.example.satellitechat.utilities.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.satellitechat.activity.client.chat.IncomingCallActivity
import com.example.satellitechat.activity.client.chat.OutGoingCallActivity
import com.example.satellitechat.utilities.constants.Constants

class DeclineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.action) {
            Constants.ACTION_DECLINE -> {
                val answerIntent = Intent(context, IncomingCallActivity::class.java).apply {
                    putExtra(Constants.SENDER_ID, intent.getStringExtra(Constants.SENDER_ID))
                    putExtra(Constants.SENDER_NAME, intent.getStringExtra(Constants.SENDER_NAME))
                    putExtra(Constants.SENDER_IMAGE, intent.getStringExtra(Constants.SENDER_IMAGE))
                    putExtra(Constants.MESSAGE_ID, intent.getStringExtra(Constants.MESSAGE_ID))
                    putExtra(Constants.RECEIVER_ID, intent.getStringExtra(Constants.RECEIVER_ID))
                    putExtra(Constants.REMOTE_MSG_MEETING_TYPE, intent.getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE))
                    putExtra(Constants.REMOTE_MSG_MEETING_ROOM, intent.getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM))
                    putExtra(Constants.REMOTE_MSG_INVITER_TOKEN, intent.getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN))
                    putExtra(Constants.PENDING_INTENT_REQUEST, intent.getStringExtra(Constants.PENDING_INTENT_REQUEST))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context?.startActivity(answerIntent)
            }
        }
    }
}
