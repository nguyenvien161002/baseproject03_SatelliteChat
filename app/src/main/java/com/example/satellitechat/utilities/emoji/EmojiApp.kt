package com.example.satellitechat.utilities.emoji

import android.app.Application
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.android.synthetic.main.activity_chat.*

class EmojiApp: Application() {

    override fun onCreate() {
        super.onCreate()
        EmojiManager.install(GoogleEmojiProvider())
    }

}