package com.example.satellitechat.listeners

import com.example.satellitechat.model.User

interface UsersListener {

    fun initiateVideoMeeting (user: User)

    fun initiateAudioMeeting (user: User)

}