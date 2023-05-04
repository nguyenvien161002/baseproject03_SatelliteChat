package com.example.satellitechat.model

import java.io.Serializable

data class User (
    var userId: String = "",
    var userName: String = "",
    var userImage: String = "",
    var userEmail: String = "",
    var userState: Any = "",
    var fcmToken: String = ""
) : Serializable