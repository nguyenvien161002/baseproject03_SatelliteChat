package com.example.satellitechat.activity.client.chat

import android.Manifest
import android.app.AlarmManager
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
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.example.satellitechat.model.User
import com.example.satellitechat.services.api.ApiService
import com.example.satellitechat.services.remote.agora.AgoraService
import com.example.satellitechat.services.remote.RetrofitService
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_incoming_call.*
import kotlinx.android.synthetic.main.activity_incoming_call.fullLayout
import kotlinx.android.synthetic.main.activity_incoming_call.localVideoView
import kotlinx.android.synthetic.main.activity_incoming_call.remoteVideoView
import kotlinx.android.synthetic.main.activity_out_going_call.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.HashMap
import kotlin.properties.Delegates


class IncomingCallActivity : AppCompatActivity() {

    private  var checkSelfPermission: Boolean = false
    private lateinit var singleChatsRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var agoraService: AgoraService
    private lateinit var meetingRoom: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        checkPermission(Constants.CAMERA_PERMISSION)
        checkPermission(Constants.RECORD_AUDIO_PERMISSION)

        preferenceManager = PreferenceManager(this@IncomingCallActivity)
        meetingRoom = intent.getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM).toString()
        agoraService = AgoraService(baseContext, meetingRoom,localVideoView, remoteVideoView, checkSelfPermission)
        agoraService.onCreate()

        // Change background color StatusBar
        window.statusBarColor = ContextCompat.getColor(
            this@IncomingCallActivity,
            R.color.incoming_call_start
        )
        // Change text color StatusBar
        window.decorView.systemUiVisibility = 0

        // Cancel pendingIntent and notification
        /* Có thể do hệ thống Android lưu trữ trạng thái PendingIntent trong một khoảng thời gian.
           Vì vậy, nếu bạn sử dụng cùng một PendingIntent nhiều lần, nó có thể lấy dữ liệu từ lần trước đó.
           Để tránh vấn đề này bạn nên hủy PendingIntent khi người dùng nhấn vào notification hoặc thay đổi
           requestCode của Pendingintent đó */

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.NOTIFICATION_ID)

        val pendingIntent = PendingIntent.getBroadcast(this, Constants.PENDING_INTENT_REQUEST, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        callerName.text = intent.getStringExtra(Constants.SENDER_NAME)
        Glide.with(this@IncomingCallActivity)
            .load(intent.getStringExtra(Constants.SENDER_IMAGE))
            .placeholder(R.drawable.profile_image).into(imageCaller)
        val messageId = intent.getStringExtra(Constants.MESSAGE_ID).toString()
        Log.d("INCOMING_ACTIVITY_MSG_ID", messageId)
        // Khi người dùng bấm vào thông báo r mới bấm vào 1 trong 2 nút này.
        // Chưa xử lí khi người dùng bấm trực tiếp vào 2 nút trên Thông báo
        btnAnswer.setOnClickListener {
            sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                intent.getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)!!
            )
//            Handler(Looper.getMainLooper()).postDelayed({
//                singleChatsRef = FirebaseDatabase.getInstance().getReference("Single Chats").child(messageId)
//                singleChatsRef.child("messageRes").setValue("true")
//            }, 2000)
        }

        btnDecline.setOnClickListener {
            sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_REJECTED,
                intent.getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)!!
            )
//            Handler(Looper.getMainLooper()).postDelayed({
//                singleChatsRef = FirebaseDatabase.getInstance().getReference("Single Chats").child(messageId)
//                singleChatsRef.child("messageRes").setValue("false")
//            }, 2000)
        }

        btnCancelCall.setOnClickListener {
            sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_CANCELLED,
                intent.getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)!!
            )
        }
    }

    private fun sendInvitationResponse(type: String, receiverToken: String) {
        try {
            val tokens: JSONArray = JSONArray()
            val body: JSONObject = JSONObject()
            val data: JSONObject = JSONObject()
            // Add receiver token to tokens JSONObject
            tokens.put(receiverToken)
            // Add data to data JSONObject
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE)
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type)
            // Add data to body JSONObject
            body.put(Constants.REMOTE_MSG_DATA, data)
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)
            // Run function
            sendRemoteMessage(body.toString(), type)
        } catch (exception: Exception) {
            Toast.makeText(this@IncomingCallActivity, exception.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendRemoteMessage(remoteMessageBody: String, type: String) {
        val retrofitService = RetrofitService()
        retrofitService.getClient()!!
            .create(ApiService::class.java)
            .sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        if (type == Constants.REMOTE_MSG_INVITATION_ACCEPTED) {
                            try {
                                agoraService.joinChannel()
                                fullLayout.visibility = View.GONE
                                containerToolBar.visibility = View.VISIBLE
                            } catch (exception: Exception) {
                                Toast.makeText(this@IncomingCallActivity, exception.message.toString(), Toast.LENGTH_LONG).show()
                            }
                        } else if (type == Constants.REMOTE_MSG_INVITATION_CANCELLED) {
                            Toast.makeText(this@IncomingCallActivity, "Invitation Cancelled", Toast.LENGTH_LONG).show()
                            onBackPressed()
                        } else {
                            Toast.makeText(this@IncomingCallActivity, "Invitation Rejected", Toast.LENGTH_LONG).show()
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

    private fun checkPermission(permission: String) {
        if (permission == Constants.CAMERA_PERMISSION) {
            if (ContextCompat.checkSelfPermission(this@IncomingCallActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(
                    this@IncomingCallActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    Constants.CAMERA_PERMISSION_REQUEST
                )
            } else {
                checkSelfPermission = true
            }
        } else if (permission == Constants.RECORD_AUDIO_PERMISSION) {
            if (ContextCompat.checkSelfPermission(this@IncomingCallActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@IncomingCallActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    Constants.RECORD_AUDIO_PERMISSION_REQUEST
                )
            } else {
                checkSelfPermission = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
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
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@IncomingCallActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Request permission camera or audio
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.CAMERA_PERMISSION_REQUEST -> {
                // Agree to allow the use
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission = true
                } else { // Do not agree to allow the use
                    Toast.makeText(this@IncomingCallActivity, "Bạn đã từ chối quyền truy cập camera", Toast.LENGTH_LONG).show()
                }
                return
            }
            Constants.RECORD_AUDIO_PERMISSION_REQUEST -> {
                // Agree to allow the use
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission = true
                } else { // Do not agree to allow the use
                    Toast.makeText(this@IncomingCallActivity, "Bạn đã từ chối quyền truy cập audio", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }
}

