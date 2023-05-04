package com.example.satellitechat.activity.client.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.User
import com.example.satellitechat.services.remote.agora.AgoraService
import com.example.satellitechat.services.api.ApiService
import com.example.satellitechat.services.remote.RetrofitService
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_out_going_call.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.Normalizer
import java.util.*

class OutGoingCallActivity : AppCompatActivity() {

    private var inviterToken: String = ""
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var receiverData: User
    private lateinit var meetingRoom: String
    private lateinit var agoraService: AgoraService
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_out_going_call)

        preferenceManager = PreferenceManager(this@OutGoingCallActivity)
        meetingRoom = "${preferenceManager.getString(Constants.USER_ID)}_${UUID.randomUUID().toString().substring(0, 5)}"
        agoraService = AgoraService(baseContext, meetingRoom, localVideoView, remoteVideoView, checkSelfPermission())
        agoraService.onCreate()

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, Constants.PERMISSION_REQ_ID);
        }

        // Change background color StatusBar
        window.statusBarColor = ContextCompat.getColor(
            this@OutGoingCallActivity,
            R.color.out_going_call_start
        )
        // Change text color StatusBar
        window.decorView.systemUiVisibility = 0

        receiverData = intent.getSerializableExtra("receiver") as User
        recNaOnToolBar.text = receiverData.userName
        receiverName.text = receiverData.userName
        Glide.with(this@OutGoingCallActivity).load(receiverData.userImage).placeholder(R.drawable.profile_image).into(imageReceiver)

        // Handle click the buttons
        icon_backspace.setOnClickListener {
            onBackPressed()
        }

        cancelAudioCall.setOnClickListener {
            if (receiverData != null) {
                cancelInvitation(receiverData.fcmToken)
            }
        }

        // Get meeting type
        val meetingType = intent.getStringExtra("type")
        if (meetingType != null) {
            if (meetingType == Constants.AUDIO_CALL) {
                initiateToken (meetingType, meetingRoom)
            } else if (meetingType == Constants.VIDEO_CALL) {
                initiateToken (meetingType, meetingRoom)
            }
        }
    }

    private fun initiateToken (meetingType: String, meetingRoom: String) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener( OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM fail", "Fetching FCM registration token failed: ", task.exception)
                    return@OnCompleteListener
                }
                // Get token success
                inviterToken = task.result
                initiateMeeting(meetingType, receiverData.fcmToken, inviterToken, meetingRoom)
            })
    }

    private fun initiateMeeting (meetingType: String, receiverToken: String, inviterToken: String, meetingRoom: String) {
        try {
            val tokens: JSONArray = JSONArray()
            val body: JSONObject = JSONObject()
            val data: JSONObject = JSONObject()
            // Add receiver token to tokens JSONObject
            tokens.put(receiverToken)
            // Add data to data JSONObject
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_REQUEST)
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType)
            data.put(Constants.SENDER_ID, preferenceManager.getString(Constants.USER_ID))
            data.put(Constants.SENDER_NAME, preferenceManager.getString(Constants.USER_NAME))
            data.put(Constants.SENDER_IMAGE, preferenceManager.getString(Constants.USER_IMAGE))
            data.put(Constants.RECEIVER_ID, preferenceManager.getString(Constants.RECEIVER_ID))
            data.put(Constants.MESSAGE_ID, intent.getStringExtra("messageId"))
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken)
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom)
            // Add data to body JSONObject
            body.put(Constants.REMOTE_MSG_DATA, data)
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)
            // Run function
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_REQUEST)
        } catch (exception: Exception) {
            Toast.makeText(this@OutGoingCallActivity, exception.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendRemoteMessage (remoteMessageBody: String, type: String) {
        val retrofitService = RetrofitService()
        retrofitService.getClient()!!
            .create(ApiService::class.java)
            .sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        if (type == Constants.REMOTE_MSG_INVITATION_REQUEST) {
                            agoraService.joinChannel()
                            Toast.makeText(this@OutGoingCallActivity, "Invitation sent successfully", Toast.LENGTH_LONG).show()
                        } else if (type == Constants.REMOTE_MSG_INVITATION_RESPONSE) {
                            agoraService.leaveChannel()
                            Toast.makeText(this@OutGoingCallActivity, "Invitation cancelled", Toast.LENGTH_LONG).show()
                            onBackPressed()
                        }
                    } else {
                        Toast.makeText(this@OutGoingCallActivity, response.message(), Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(this@OutGoingCallActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun cancelInvitation (receiverToken: String) {
        try {
            val tokens: JSONArray = JSONArray()
            val body: JSONObject = JSONObject()
            val data: JSONObject = JSONObject()
            // Add receiver token to tokens JSONObject
            tokens.put(receiverToken)
            // Add data to data JSONObject
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE)
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED)
            // Add data to body JSONObject
            body.put(Constants.REMOTE_MSG_DATA, data)
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)
            // Run function
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE)
        } catch (exception: Exception) {
            Toast.makeText(this@OutGoingCallActivity, exception.message, Toast.LENGTH_LONG).show()
        }
    }

    private val invitationResponseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val type = intent!!.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE)
            if (type != null) {
                if (type == Constants.REMOTE_MSG_INVITATION_ACCEPTED) {
                    try {
                        agoraService.joinChannel()
                        receiverName.visibility = View.GONE
                        calledTime.visibility = View.GONE
                        imageReceiver.visibility = View.GONE
                    } catch (exception: Exception) {
                        Toast.makeText(this@OutGoingCallActivity, exception.message.toString(), Toast.LENGTH_LONG).show()
                    }
                } else if (type == Constants.REMOTE_MSG_INVITATION_CANCELLED) {
                    Toast.makeText(this@OutGoingCallActivity, "Invitation Cancelled", Toast.LENGTH_LONG).show()
                    onBackPressed()
                } else if (type == Constants.REMOTE_MSG_INVITATION_REJECTED) {
                    Toast.makeText(this@OutGoingCallActivity, "Invitation Rejected", Toast.LENGTH_LONG).show()
                    onBackPressed()
                }
            }
        }
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this@OutGoingCallActivity).registerReceiver(
            invitationResponseReceiver,
            IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this@OutGoingCallActivity).unregisterReceiver(
            invitationResponseReceiver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraService.onDestroy()
    }

}