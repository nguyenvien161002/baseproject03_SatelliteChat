package com.example.satellitechat.services.remote

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.satellitechat.R
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.notification.AnswerReceiver
import com.example.satellitechat.utilities.notification.DeclineReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    @SuppressLint("LaunchActivityFromNotification")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type: String? = remoteMessage.data[Constants.REMOTE_MSG_TYPE]

        if (type != null) {
            if (type == Constants.REMOTE_MSG_INVITATION_REQUEST) {

                val senderId = remoteMessage.data[Constants.SENDER_ID]
                val senderName = remoteMessage.data[Constants.SENDER_NAME]
                val senderImage = remoteMessage.data[Constants.SENDER_IMAGE]
                val receiverId = remoteMessage.data[Constants.RECEIVER_ID]
                val messageId = remoteMessage.data[Constants.MESSAGE_ID]
                val meetingType = remoteMessage.data[Constants.REMOTE_MSG_MEETING_TYPE]
                val meetingRoom = remoteMessage.data[Constants.REMOTE_MSG_MEETING_ROOM]
                val inviterToken = remoteMessage.data[Constants.REMOTE_MSG_INVITER_TOKEN]
                val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
                val pendingIntentRequest = generateRandomNumber()
                val channel = NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = Constants.CHANNEL_DESCRIPTION
                channel.setShowBadge(true)
                notificationManager.createNotificationChannel(channel)

                /* -----NOTE-----
                Có thể do hệ thống Android lưu trữ trạng thái PendingIntent trong một khoảng thời gian.
                Vì vậy, nếu bạn sử dụng cùng một PendingIntent (có cùng requestCode) nhiều lần, nó có thể lấy
                dữ liệu từ lần trước đó. Để tránh vấn đề này, bạn có thể thêm một đối số requestCode khác nhau cho mỗi PendingIntent.
                Điều này sẽ đảm bảo rằng mỗi PendingIntent sẽ có một trạng thái lưu trữ riêng biệt, ngay cả khi chúng có cùng Intent.
                Trong đó, requestCode là một số nguyên duy nhất để xác định PendingIntent khác nhau. Bạn có thể sử dụng một giá trị bất kỳ cho requestCode,
                miễn là chúng khác nhau cho mỗi PendingIntent.
                */

                // Init pendingIntentAnswer
                val answerIntent = Intent(this, AnswerReceiver::class.java).apply {
                    putExtra(Constants.SENDER_ID, senderId)
                    putExtra(Constants.SENDER_NAME, senderName)
                    putExtra(Constants.SENDER_IMAGE, senderImage)
                    putExtra(Constants.MESSAGE_ID, messageId)
                    putExtra(Constants.RECEIVER_ID, receiverId)
                    putExtra(Constants.REMOTE_MSG_MEETING_TYPE, meetingType)
                    putExtra(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom)
                    putExtra(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    action = Constants.ACTION_ANSWER
                }
                val pendingIntentAnswer = PendingIntent.getBroadcast(this, pendingIntentRequest, answerIntent, PendingIntent.FLAG_IMMUTABLE)

                // Init pendingIntentDecline
                val declineIntent = Intent(this, DeclineReceiver::class.java).apply {
                    putExtra(Constants.SENDER_ID, senderId)
                    putExtra(Constants.SENDER_NAME, senderName)
                    putExtra(Constants.SENDER_IMAGE, senderImage)
                    putExtra(Constants.MESSAGE_ID, messageId)
                    putExtra(Constants.RECEIVER_ID, receiverId)
                    putExtra(Constants.REMOTE_MSG_MEETING_TYPE, meetingType)
                    putExtra(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom)
                    putExtra(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken)
                    action = Constants.ACTION_DECLINE
                }
                val pendingIntentDecline = PendingIntent.getBroadcast(this, pendingIntentRequest, declineIntent, PendingIntent.FLAG_IMMUTABLE)

                // Init notification
                val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon_call)
                    .setContentTitle(senderName)
                    .setContentText(getString(R.string.call_notification))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntentAnswer)
                    .setAutoCancel(true)
                    .addAction(R.drawable.icon_call, "Answer", pendingIntentAnswer)
                    .addAction(R.drawable.icons_phone_white, "Decline", pendingIntentDecline)
                    .build()

                notificationManager.notify(Constants.NOTIFICATION_ID, notification)
            } else if (type == Constants.REMOTE_MSG_INVITATION_RESPONSE) {
                val intent = Intent(Constants.REMOTE_MSG_INVITATION_RESPONSE).apply {
                    putExtra(
                        Constants.REMOTE_MSG_INVITATION_RESPONSE,
                        remoteMessage.data[Constants.REMOTE_MSG_INVITATION_RESPONSE]
                    )
                }
                LocalBroadcastManager.getInstance(this@MessagingService).sendBroadcast(intent)
            }
        }
    }

    private fun generateRandomNumber(): Int {
        val random = Random()
        return random.nextInt(90000) + 10000
    }

}