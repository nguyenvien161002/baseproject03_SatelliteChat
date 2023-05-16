package com.example.satellitechat.utilities.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.satellitechat.R
import com.example.satellitechat.utilities.constants.Constants
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CallNotification(private var context: Context) {

    /**
     * Sử dụng một lớp singleton để duy trì một thể hiện duy nhất của lớp CallNotification trong suốt
     * vòng đời của ứng dụng. Bằng cách này sẽ đảm bảo rằng cả MessagingService và IncomingCallActivity
     * đang tham chiếu đến cùng một đối tượng CallNotification. Như vậy sẽ xóa được đúng callbackShowMissedCall,
     * nếu ta không dùng singleton thì khi ta new một đối tượng CallNotification bên MessagingService hay IncomingCallActivity
     * thì hai đối tượng này độc lập, không liên quan đến nhau nên khi xóa callbackShowMissedCall sẽ không được
     * **/
    companion object {
        @Volatile
        private var instance: CallNotification? = null
        fun getInstance(context: Context): CallNotification {
            return instance ?: synchronized(this) {
                instance ?: CallNotification(context).also { instance = it }
            }
        }
    }

    private var callerNameN: String = ""
    private var messageIdN: String = ""
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var notificationManager = NotificationManagerCompat.from(context)
    private val channel = NotificationChannel(
        Constants.CHANNEL_ID,
        Constants.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    )
    private var callbackShowMissedCall: Runnable = Runnable {
        notificationManager.cancel(Constants.NOTIFICATION_ID_CALL)
        showMissedCallNotification(callerNameN)
    }
    private lateinit var messagesRef: DatabaseReference


    fun onCreate() {
        channel.description = Constants.CHANNEL_DESCRIPTION
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)
    }

    fun removeShowMissedCallNotification() {
        handler.removeCallbacks(callbackShowMissedCall)
        notificationManager.cancel(Constants.NOTIFICATION_ID_CALL)
        notificationManager.cancel(Constants.NOTIFICATION_ID_MISSED_CALL)
    }

    private fun showMissedCallNotification(callerName: String) {
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID)
            .setSmallIcon(R.drawable.icons_missed_call_50)
            .setContentTitle("Missed call")
            .setContentText("You missed a call $callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_MISSED_CALL, notification)
        messagesRef = FirebaseDatabase.getInstance().getReference(Constants.MESSAGES_REF)
        messagesRef.child(messageIdN).child("messageRes").setValue("missed_call")
    }

    fun showCallNotification(
        messageId: String, callerName: String, meetingType: String,
        pendingIntentAnswer: PendingIntent, pendingIntentDecline: PendingIntent
    ) {
        callerNameN = callerName
        messageIdN = messageId
        val meetingTypeN = if (meetingType == Constants.AUDIO_CALL) {
            "Calling"
        } else {
            "Video chatting"
        }
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID)
            .setSmallIcon(R.drawable.icons_phone_rotate_45)
            .setContentTitle(callerName)
            .setContentText("$meetingTypeN from SatelliteChat")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntentAnswer)
            .setAutoCancel(true)
            .addAction(R.drawable.icon_call, "Answer", pendingIntentAnswer)
            .addAction(R.drawable.icons_phone_white, "Decline", pendingIntentDecline)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_MISSED_CALL, notification)
        handler.postDelayed(callbackShowMissedCall,15000)
    }
}

