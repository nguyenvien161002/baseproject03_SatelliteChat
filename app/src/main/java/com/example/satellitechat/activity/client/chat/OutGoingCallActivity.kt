package com.example.satellitechat.activity.client.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Time
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.example.satellitechat.utilities.send.SendMessage
import com.example.satellitechat.utilities.time.TimeAndDateGeneral
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_out_going_call.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.HashMap

class OutGoingCallActivity : AppCompatActivity() {

    private var inviterToken: String = ""
    private var currentUserId: String = ""
    private var messageId: String = ""
    private var timeStartCall: Long = 0
    private var checkResponseCall: String = ""
    private var meetingRoom: String = ""
    private var messageState: HashMap<String, Any> =
        TimeAndDateGeneral().getCurrentTimeAndDate()
    private var ringingTimeCallHandler: Handler? = null
    private var ringingTimeCallRunnable = Runnable {
        handlerRunnableRingingTimeCall()
    }
    private var callDurationHandler: Handler? = null
    private var timeAndDateGeneral: TimeAndDateGeneral = TimeAndDateGeneral()
    private val callDurationRunnable = object : Runnable {
        override fun run() {
            val timeInMillis = System.currentTimeMillis() - timeStartCall
            calledTime.text = timeAndDateGeneral.timeFormatDurationCall(timeInMillis)
            callDurationHandler?.postDelayed(this, 1000)
        }
    }
    private lateinit var send: SendMessage
    private lateinit var receiverData: User
    private lateinit var agoraService: AgoraService
    private lateinit var messagesRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_out_going_call)

        val meetingType = intent.getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE)!!
        preferenceManager = PreferenceManager(this@OutGoingCallActivity)
        meetingRoom = "${preferenceManager.getString(Constants.USER_ID)}_${UUID.randomUUID().toString().substring(0, 5)}"
        send = SendMessage()
        messagesRef = FirebaseDatabase.getInstance().getReference(Constants.MESSAGES_REF)
        agoraService = AgoraService(baseContext, meetingRoom, localVideoView, remoteVideoView, meetingType)
        agoraService.onCreate()

        // Change background color StatusBar
        window.statusBarColor = ContextCompat.getColor(
            this@OutGoingCallActivity,
            R.color.out_going_call_start
        )
        // Change text color StatusBar
        window.decorView.systemUiVisibility = 0

        currentUserId = preferenceManager.getCurrentId().toString()
        receiverData = intent.getSerializableExtra("receiver") as User
        recNaOnToolBar.text = receiverData.userName
        receiverName.text = receiverData.userName
        Glide.with(this@OutGoingCallActivity).load(receiverData.userImage)
            .placeholder(R.drawable.profile_image).into(imageReceiver)

        // Handle click the buttons
        icon_backspace.setOnClickListener {
            onBackPressed()
        }
        // Get meeting type
        cancelAudioCall.setOnClickListener {
            if (receiverData != null) {
                cancelInvitation(receiverData.fcmToken, meetingType)
            }
        }

        if (meetingType != null) {
            if (meetingType == Constants.AUDIO_CALL) {
                send.sendMessage(
                    currentUserId, receiverData.userId, "Audio Call",
                    "audio_call", messageState, "", HashMap()
                )
                messageId = send.getMessageId()
                initiateToken (meetingType, meetingRoom, messageId)
            } else if (meetingType == Constants.VIDEO_CALL) {
                send.sendMessage(
                    currentUserId, receiverData.userId, "Video Call",
                    "video_call", messageState, "", HashMap()
                )
                messageId = send.getMessageId()
                initiateToken (meetingType, meetingRoom, messageId)
            }
        }
    }

    private fun initiateToken (meetingType: String, meetingRoom: String, messageId: String) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener( OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM fail", "Fetching FCM registration token failed: ", task.exception)
                    return@OnCompleteListener
                }
                // Get token success
                inviterToken = task.result
                initiateMeeting(meetingType, receiverData.fcmToken, inviterToken, meetingRoom, messageId)
            })
    }

    private fun initiateMeeting (meetingType: String, receiverToken: String, inviterToken: String, meetingRoom: String, messageId: String) {
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
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken)
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom)
            data.put(Constants.MESSAGE_ID, messageId)
            // Add data to body JSONObject
            body.put(Constants.REMOTE_MSG_DATA, data)
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)
            // Run function
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_REQUEST, meetingType)
        } catch (exception: Exception) {
            Toast.makeText(this@OutGoingCallActivity, exception.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendRemoteMessage (remoteMessageBody: String, type: String, meetingType: String) {
        val retrofitService = RetrofitService()
        retrofitService.getClient()!!
            .create(ApiService::class.java)
            .sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        if (type == Constants.REMOTE_MSG_INVITATION_REQUEST) {
                            if(meetingType == Constants.VIDEO_CALL) {
                                agoraService.joinChannelVideoCall()
                            } else if (meetingType == Constants.AUDIO_CALL) {
                                agoraService.joinChannelAudioCall()
                            }
                            Toast.makeText(this@OutGoingCallActivity, "Invitation sent successfully", Toast.LENGTH_LONG).show()
                        } else if (type == Constants.REMOTE_MSG_INVITATION_RESPONSE) {
                            if(meetingType == Constants.VIDEO_CALL) {
                                agoraService.leaveChannelVideoCall()
                            } else if (meetingType == Constants.AUDIO_CALL) {
                                agoraService.leaveChannelAudioCall()
                            }
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

    private fun cancelInvitation (receiverToken: String, meetingType: String) {
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
            if (checkResponseCall == "true") {
                val hashMap: HashMap<String, Any> = HashMap()
                val timeFinishCall = System.currentTimeMillis()
                val tempTimeCallDuration = timeFinishCall - timeStartCall
                val timeCallDuration = timeAndDateGeneral.timeFormatDurationCall(tempTimeCallDuration)
                hashMap["messageRes"] = "caller_cancelled"
                hashMap["messageCaDur"] = timeCallDuration
                messagesRef.child(messageId).child("messageRes").updateChildren(hashMap)
            } else if (checkResponseCall == "false") {
                messagesRef.child(messageId).child("messageRes").setValue("caller_cancelled")
            }
            // Run function
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE, meetingType)
        } catch (exception: Exception) {
            Toast.makeText(this@OutGoingCallActivity, exception.message, Toast.LENGTH_LONG).show()
        }
    }

    private val invitationResponseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            ringingTimeCallHandler?.removeCallbacks(ringingTimeCallRunnable)
            val type = intent!!.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE)
            val meetingType = intent.getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE)
            if (type != null) {
                if (type == Constants.REMOTE_MSG_INVITATION_ACCEPTED) {
                    try {
                        checkResponseCall = "true"
                        timeStartCall = System.currentTimeMillis()
                        callDurationHandler = Handler(Looper.getMainLooper())
                        callDurationHandler?.postDelayed(callDurationRunnable, 0)
                        if (meetingType == Constants.VIDEO_CALL) {
                            agoraService.joinChannelVideoCall()
                            receiverName.visibility = View.GONE
                            calledTime.visibility = View.GONE
                            imageReceiver.visibility = View.GONE
                        } else {
                            agoraService.joinChannelAudioCall()
                            receiverName.visibility = View.VISIBLE
                            calledTime.visibility = View.VISIBLE
                            imageReceiver.visibility = View.VISIBLE
                        }
                    } catch (exception: Exception) {
                        Toast.makeText(this@OutGoingCallActivity, exception.message.toString(), Toast.LENGTH_LONG).show()
                    }
                } else if (type == Constants.REMOTE_MSG_INVITATION_CANCELLED) {
                    val hashMap: HashMap<String, Any> = HashMap()
                    val timeFinishCall = System.currentTimeMillis()
                    val tempTimeCallDuration = timeFinishCall - timeStartCall
                    val timeCallDuration = timeAndDateGeneral.timeFormatDurationCall(tempTimeCallDuration)
                    hashMap["messageRes"] = "receiver_cancelled"
                    hashMap["messageCaDur"] = timeCallDuration
                    messagesRef.child(messageId).child("messageRes").updateChildren(hashMap)
                    Toast.makeText(this@OutGoingCallActivity, "Invitation Cancelled", Toast.LENGTH_LONG).show()
                    onBackPressed()
                } else if (type == Constants.REMOTE_MSG_INVITATION_REJECTED) {
                    messagesRef.child(messageId).child("messageRes").setValue("receiver_rejected")
                    Toast.makeText(this@OutGoingCallActivity, "Invitation Rejected", Toast.LENGTH_LONG).show()
                    onBackPressed()
                }
            }
        }
    }

    private fun ringingTimeCall() {
        ringingTimeCallHandler = Handler(Looper.getMainLooper())
        ringingTimeCallHandler?.postDelayed(ringingTimeCallRunnable, 16000)
    }

    private fun handlerRunnableRingingTimeCall() {
        messagesRef.child(messageId).child("messageRes").setValue("missed_call")
        onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        ringingTimeCall()
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
        callDurationHandler?.removeCallbacks(callDurationRunnable)
        ringingTimeCallHandler?.removeCallbacks(ringingTimeCallRunnable)
    }

}