package com.example.satellitechat.services.remote.agora

import android.content.Context
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.example.satellitechat.R
import com.example.satellitechat.services.remote.agora.media.RtcTokenBuilder2
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import com.example.satellitechat.utilities.constants.Constants.AGORA_APP_ID
import com.example.satellitechat.utilities.constants.Constants.AGORA_APP_CERTIFICATE

class AgoraService (
    private var baseContext: Context,
    private var channelName: String,
    private var localViewVideo: FrameLayout,
    private var remoteViewVideo: FrameLayout,
    private var checkSelfPermission: Boolean
) {

    private var appId:String = AGORA_APP_ID
    private var appCertificate = AGORA_APP_CERTIFICATE
    private var token: String? = null
    private var uid = 0
    private var expirationTimeInSeconds: Int = 3600
    private var isJoined: Boolean = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    fun onCreate() {
        setupVideoSDKEngine();
        val tokenBuilder = RtcTokenBuilder2()
        val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
        token = tokenBuilder.buildTokenWithUid(
            appId, appCertificate,
            channelName, uid, RtcTokenBuilder2.Role.ROLE_PUBLISHER, timestamp, timestamp
        )
    }

    fun onDestroy() {
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (e: java.lang.Exception) {
            Toast.makeText(baseContext, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.layoutParams = FrameLayout.LayoutParams (
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        remoteSurfaceView!!.holder.setFixedSize(
            remoteViewVideo.width,
            remoteViewVideo.height
        )
        remoteViewVideo.setBackgroundResource(R.drawable.bg_radius8_video_call);
        remoteViewVideo.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        localSurfaceView!!.setZOrderMediaOverlay(true)
        localViewVideo.setBackgroundResource(R.drawable.bg_radius8_video_call);
        localViewVideo.addView(localSurfaceView)
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    fun joinChannel() {
        if (checkSelfPermission) {
            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = View.VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, options)
        } else {
            Toast.makeText(baseContext, "Permissions was not granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun leaveChannel() {
        if (!isJoined) {
            Toast.makeText(baseContext, "Join a channel first", Toast.LENGTH_SHORT).show()
        } else {
            agoraEngine!!.leaveChannel()
            Toast.makeText(baseContext, "You left the channel", Toast.LENGTH_SHORT).show()
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
    }
}