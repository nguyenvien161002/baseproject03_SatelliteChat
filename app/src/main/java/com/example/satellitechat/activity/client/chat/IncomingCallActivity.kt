package com.example.satellitechat.activity.client.chat

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.example.satellitechat.services.api.ApiService
import com.example.satellitechat.services.remote.RetrofitService
import com.example.satellitechat.services.remote.agora.AgoraService
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.notification.CallNotification
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.example.satellitechat.utilities.time.TimeAndDateGeneral
import com.google.firebase.database.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.sql.Time


class IncomingCallActivity : AppCompatActivity() {

    private var timeStartCall: Long = 0
    private var meetingRoom: String = ""
    private var meetingType: String = ""
    private var inviterToken: String = ""
    private var callerNameFrIntent: String = ""
    private var timeAndDateGeneral: TimeAndDateGeneral = TimeAndDateGeneral()
    private var ringingTimeCallHandler: Handler? = null
    private var ringingTimeCallRunnable = Runnable { onBackPressed() }
    private var callDurationHandler: Handler? = null
    private val callDurationRunnable = object : Runnable {
        override fun run() {
            val timeInMillis = System.currentTimeMillis() - timeStartCall
            labelAudioCallOrTimeCall.text = timeAndDateGeneral.timeFormatDurationCall(timeInMillis)
            callDurationHandler?.postDelayed(this, 1000)
        }
    }
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var agoraService: AgoraService
    private lateinit var callNotification: CallNotification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        inviterToken = intent.getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)!!
        meetingType = intent.getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE)!!
        preferenceManager = PreferenceManager(this@IncomingCallActivity)
        meetingRoom = intent.getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM).toString()
        agoraService = AgoraService(baseContext, meetingRoom,localVideoView, remoteVideoView, meetingType)
        agoraService.onCreate()
        callNotification = CallNotification.getInstance(baseContext)
        callNotification.onCreate()
        callNotification.removeShowMissedCallNotification()

        // Change background color StatusBar
        window.statusBarColor = ContextCompat.getColor(
            this@IncomingCallActivity,
            R.color.incoming_call_start
        )
        // Change text color StatusBar
        window.decorView.systemUiVisibility = 0

        /* Có thể do hệ thống Android lưu trữ trạng thái PendingIntent trong một khoảng thời gian.
           Vì vậy, nếu bạn sử dụng cùng một PendingIntent nhiều lần, nó có thể lấy dữ liệu từ lần trước đó.
           Để tránh vấn đề này bạn nên hủy PendingIntent khi người dùng nhấn vào notification hoặc thay đổi
           requestCode của Pendingintent đó */
        val pendingIntentRequest = intent.getIntExtra(Constants.PENDING_INTENT_REQUEST, Constants.PENDING_INTENT_REQUEST_CODE)
        val pendingIntent = PendingIntent.getBroadcast(this, pendingIntentRequest, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        callerNameFrIntent = intent.getStringExtra(Constants.SENDER_NAME).toString()
        callerName.text = callerNameFrIntent
        Glide.with(this@IncomingCallActivity)
            .load(intent.getStringExtra(Constants.SENDER_IMAGE))
            .placeholder(R.drawable.profile_image).into(imageCaller)

        // Khi người dùng bấm vào thông báo r mới bấm vào 1 trong 2 nút này.
        // Chưa xử lí khi người dùng bấm trực tiếp vào 2 nút trên Thông báo
        btnAnswer.setOnClickListener {
            handlerBtnAnswerCall(meetingType)
        }

        btnDecline.setOnClickListener {
            sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_REJECTED,
                inviterToken,
                meetingType
            )
        }

        btnCancelCall.setOnClickListener {
            sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_CANCELLED,
                inviterToken,
                meetingType
            )
        }
    }

    private fun sendInvitationResponse(type: String, receiverToken: String, meetingType: String) {
        try {
            val tokens: JSONArray = JSONArray()
            val body: JSONObject = JSONObject()
            val data: JSONObject = JSONObject()
            // Add receiver token to tokens JSONObject
            tokens.put(receiverToken)
            // Add data to data JSONObject
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE)
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type)
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType)
            // Add data to body JSONObject
            body.put(Constants.REMOTE_MSG_DATA, data)
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)
            // Run function
            sendRemoteMessage(body.toString(), type, meetingType)
        } catch (exception: Exception) {
            Toast.makeText(this@IncomingCallActivity, exception.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendRemoteMessage(remoteMessageBody: String, type: String, meetingType: String) {
        val retrofitService = RetrofitService()
        retrofitService.getClient()!!
            .create(ApiService::class.java)
            .sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        if (type == Constants.REMOTE_MSG_INVITATION_ACCEPTED) {
                            try {
                                timeStartCall = System.currentTimeMillis()
                                callDurationHandler = Handler(Looper.getMainLooper())
                                callDurationHandler?.postDelayed(callDurationRunnable, 0)
                                if (meetingType == Constants.VIDEO_CALL) {
                                    agoraService.joinChannelVideoCall()
                                    fullLayout.visibility = View.GONE
                                    containerToolBar.visibility = View.VISIBLE
                                } else if (meetingType == Constants.AUDIO_CALL)  {
                                    agoraService.joinChannelAudioCall()
                                    fullLayout.visibility = View.VISIBLE
                                    containerTitleResponseCall.visibility = View.GONE
                                    containerBtnAnswerDecline.visibility = View.GONE
                                    containerToolBar.visibility = View.VISIBLE
                                }
                            } catch (exception: Exception) {
                                Toast.makeText(this@IncomingCallActivity, exception.message.toString(), Toast.LENGTH_LONG).show()
                            }
                        } else if (type == Constants.REMOTE_MSG_INVITATION_CANCELLED) {
                            Toast.makeText(this@IncomingCallActivity, "Invitation Cancelled IncomingActivity", Toast.LENGTH_LONG).show()
                            onBackPressed()
                        } else {
                            Toast.makeText(this@IncomingCallActivity, "Invitation Rejected IncomingActivity", Toast.LENGTH_LONG).show()
                            onBackPressed()
                        }
                    } else {
                        Toast.makeText(this@IncomingCallActivity, response.message(), Toast.LENGTH_LONG).show()
                        onBackPressed()
                    }
                }
                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(this@IncomingCallActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun checkPermission(permission: String) {
        if (permission == Constants.RECORD_AUDIO_PERMISSION) {
            if (ContextCompat.checkSelfPermission(this@IncomingCallActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@IncomingCallActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    Constants.RECORD_AUDIO_PERMISSION_REQUEST
                )
            } else {
                handlerSendInvitationResponse()
            }
        } else if (permission == Constants.CAMERA_RECORD_AUDIO_PERMISSION) {
            if ((ContextCompat.checkSelfPermission(this@IncomingCallActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this@IncomingCallActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
                val arrayPermission: Array<String> = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
                ActivityCompat.requestPermissions(
                    this@IncomingCallActivity,
                    arrayPermission,
                    Constants.CAMERA_RECORD_AUDIO_PERMISSION_REQUEST
                )
            } else {
                handlerSendInvitationResponse()
            }
        }
    }

    private fun handlerSendInvitationResponse() {
        sendInvitationResponse(
            Constants.REMOTE_MSG_INVITATION_ACCEPTED,
            inviterToken,
            meetingType
        )
    }

    private fun ringingTimeCall() {
        ringingTimeCallHandler = Handler(Looper.getMainLooper())
        ringingTimeCallHandler?.postDelayed(ringingTimeCallRunnable, 16000)
    }

    private fun handlerBtnAnswerCall(meetingType: String) {
        if (meetingType == Constants.AUDIO_CALL) {
            checkPermission(Constants.RECORD_AUDIO_PERMISSION)
        } else {
            checkPermission(Constants.CAMERA_RECORD_AUDIO_PERMISSION)
        }
        ringingTimeCallHandler?.removeCallbacks(ringingTimeCallRunnable)
    }

    // Người gọi gọi đến và nhấn nút cancel cuộc gọi khi bắt máy or chưa bắt máy
    private val invitationResponseCaller: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val type = intent!!.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE)
            if (type != null) {
                if (type == Constants.REMOTE_MSG_INVITATION_CANCELLED) {
                    Toast.makeText(this@IncomingCallActivity, "Invitation Cancelled", Toast.LENGTH_LONG).show()
                    onBackPressed()
                }
            }

        }
    }

    override fun onStart() {
        super.onStart()
        ringingTimeCall()
        LocalBroadcastManager.getInstance(this@IncomingCallActivity).registerReceiver(
            invitationResponseCaller,
            IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this@IncomingCallActivity).unregisterReceiver(
            invitationResponseCaller
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraService.onDestroy()
        callDurationHandler?.removeCallbacks(callDurationRunnable)
        ringingTimeCallHandler?.removeCallbacks(ringingTimeCallRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.RECORD_AUDIO_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handlerSendInvitationResponse()
                } else { // Do not agree to allow the use
                    Toast.makeText(this@IncomingCallActivity, "Bạn đã từ chối quyền truy cập record audio", Toast.LENGTH_LONG).show()
                }
            }
            Constants.CAMERA_RECORD_AUDIO_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) ||
                    (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    handlerSendInvitationResponse()
                } else { // Do not agree to allow the use
                    Toast.makeText(this@IncomingCallActivity, "Bạn đã từ chối quyền truy cập audio và camera", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@IncomingCallActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}

