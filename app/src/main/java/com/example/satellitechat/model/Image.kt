package com.example.satellitechat.model

import android.net.Uri

data class Image (
    var uri: Uri? = null, // Send images and render real-time using uri
    var url: String? = null // Render images using url from DB
)