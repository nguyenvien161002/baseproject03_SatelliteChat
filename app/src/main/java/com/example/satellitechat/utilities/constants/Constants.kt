package com.example.satellitechat.utilities.constants

object Constants {

    // Collection Firebase
    const val USERS_REF: String = "Users"
    const val MESSAGES_REF: String = "Messages"
    const val STATUSES_REF: String = "Statuses"
    const val STATUS_PERMISSION: String = "status_permission"
    const val STATUS_ID: String = "status_id"

    const val USER_ID: String = "user_id"
    const val USER_NAME: String = "user_name"
    const val USER_EMAIL: String = "user_email"
    const val USER_PASSWORD: String = "user_password"
    const val USER_IMAGE: String = "user_image"
    const val RECEIVER_ID: String = "receiver_id"
    const val SENDER_ID: String = "sender_id"
    const val SENDER_NAME: String = "sender_name"
    const val SENDER_IMAGE: String = "sender_image"
    const val MESSAGE_ID: String = "message_id"
    const val CHOOSE_FILE_REQUEST: Int = 1000
    const val CHOOSE_FILE_RESPONSE: Int = 1001
    const val FILE_PERMISSION_REQUEST: Int = 3002
    const val CHOOSE_IMAGE_REQUEST: Int = 2002
    const val CHOOSE_IMAGE_RESPONSE: Int = 2003
    const val IMAGE_PERMISSION_REQUEST: Int = 3001
    const val IMAGE_PERMISSION: String = "image_permission"
    const val FILE_PERMISSION: String = "file_permission"
    const val CAMERA_PERMISSION: String = "camera_permission"
    const val CAMERA_PERMISSION_REQUEST: Int = 4001
    const val NOTIFICATION_PERMISSION: String = "notification_permission"
    const val NOTIFICATION_PERMISSION_REQUEST: Int = 5001
    const val RECORD_AUDIO_PERMISSION: String = "record_audio_permission"
    const val RECORD_AUDIO_PERMISSION_REQUEST: Int = 6001
    const val PREFERENCE_NAME: String = "satellite_chat_preference"
    const val IS_SIGN_IN: String = "is_sign_in"
    const val METHOD_SIGN_IN: String = "method_sign_in"
    const val IMAGE_CAPTURE_REQUEST: Int = 3000
    const val AUDIO_CALL: String = "audio"
    const val VIDEO_CALL: String = "video"
    const val CHANNEL_NAME: String = "SatelliteChat"
    const val CHANNEL_ID: String = "16102002"
    const val CHANNEL_DESCRIPTION: String = "SatelliteChat"
    const val GG_API_FCM_BASE_URL: String = "https://fcm.googleapis.com/fcm/"
    const val REMOTE_MSG_TYPE: String = "type"
    const val REMOTE_MSG_INVITATION_REQUEST: String = "invitationRequest"
    const val REMOTE_MSG_MEETING_TYPE: String = "meetingType"
    const val REMOTE_MSG_INVITER_TOKEN: String = "inviterToken"
    const val REMOTE_MSG_DATA: String = "data"
    const val REMOTE_MSG_REGISTRATION_IDS: String = "registration_ids"
    const val NOTIFICATION_ID: Int = 1000
    const val PENDING_INTENT_REQUEST: Int = 4006
    const val ACTION_ANSWER: String = "com.example.satellitechat.ACTION_ANSWER"
    const val ACTION_DECLINE: String = "com.example.satellitechat.ACTION_DECLINE"
    const val REMOTE_MSG_INVITATION_RESPONSE: String = "invitationResponse"
    const val REMOTE_MSG_INVITATION_ACCEPTED: String = "accepted"
    const val REMOTE_MSG_INVITATION_REJECTED: String = "rejected"
    const val REMOTE_MSG_INVITATION_CANCELLED: String = "cancelled"
    const val REMOTE_MSG_MEETING_ROOM: String = "meetingRoom"
    const val AGORA_APP_ID: String  = "11aec03213a849be9342f8d774c773ed"
    const val AGORA_APP_CERTIFICATE: String = "390d7f6f60be42928d7ef65fefe6b487"
    const val PERMISSION_REQ_ID = 22

    // Private
    private const val REMOTE_MSG_AUTHORIZATION: String = "Authorization"
    private const val REMOTE_MSG_CONTENT_TYPE: String = "Content-type"
    private const val SERVER_KEY_TOKEN : String = "AAAAPkzXe_k:APA91bHXx1wE66sm0eFpKFwfeuaTQBAeMkYxQHk89Ui5iYyB3i_c6zFWRWatX0SL5lZ7ztnn3-6ATWdRTYiEAyxxan3_gdn0RHDnTYsDD4jj0swUnjwMtI-uxRZPLtqevogvdUyXxjCX"

    fun getRemoteMessageHeaders (): HashMap<String, String> {
        val headers: HashMap<String, String> = HashMap()
        headers[REMOTE_MSG_AUTHORIZATION] = "key=$SERVER_KEY_TOKEN"
        headers[REMOTE_MSG_CONTENT_TYPE] = "application/json"
        return headers
    }

}

