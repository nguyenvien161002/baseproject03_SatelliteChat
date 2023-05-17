package com.example.satellitechat.model

data class Relationship (
    var relationshipId: String = "",
    var requestSenderId: String = "",
    var requestReceiverId: String = "",
    var requestStatus: String = "",
    var requestSentTime: Any = "",
    var requestAcceptedTime: Any = "",
    var requestRejectedTime: Any = "",
    var requestCancelledTime: Any = ""
)