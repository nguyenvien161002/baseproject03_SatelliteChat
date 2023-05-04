package com.example.satellitechat.services.remote.agora.media

import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class AccessToken2 {
    enum class PrivilegeRtc(value: Int) {
        PRIVILEGE_JOIN_CHANNEL(1), PRIVILEGE_PUBLISH_AUDIO_STREAM(2), PRIVILEGE_PUBLISH_VIDEO_STREAM(
            3
        ),
        PRIVILEGE_PUBLISH_DATA_STREAM(4);

        var intValue: Short

        init {
            intValue = value.toShort()
        }
    }

    enum class PrivilegeRtm(value: Int) {
        PRIVILEGE_LOGIN(1);

        var intValue: Short

        init {
            intValue = value.toShort()
        }
    }

    enum class PrivilegeFpa(value: Int) {
        PRIVILEGE_LOGIN(1);

        var intValue: Short

        init {
            intValue = value.toShort()
        }
    }

    enum class PrivilegeChat(value: Int) {
        PRIVILEGE_CHAT_USER(1), PRIVILEGE_CHAT_APP(2);

        var intValue: Short

        init {
            intValue = value.toShort()
        }
    }

    enum class PrivilegeEducation(value: Int) {
        PRIVILEGE_ROOM_USER(1), PRIVILEGE_USER(2), PRIVILEGE_APP(3);

        var intValue: Short

        init {
            intValue = value.toShort()
        }
    }

    var appCert = ""
    var appId = ""
    var expire = 0
    var issueTs = 0
    var salt = 0
    var services: MutableMap<Short, Service> = TreeMap()

    constructor() {}
    constructor(appId: String, appCert: String, expire: Int) {
        this.appCert = appCert
        this.appId = appId
        this.expire = expire
        issueTs = Utils.timestamp
        salt = Utils.randomInt()
    }

    fun addService(service: Service) {
        services[service.serviceType] = service
    }

    @Throws(Exception::class)
    fun build(): String {
        if (!Utils.isUUID(appId) || !Utils.isUUID(appCert)) {
            return ""
        }
        val buf: ByteBuf =
            ByteBuf().put(appId).put(issueTs).put(expire).put(salt).put(services.size.toShort())
        val signing = sign
        services.forEach { (k: Short?, v: Service) ->
            v.pack(
                buf
            )
        }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signing, "HmacSHA256"))
        val signature = mac.doFinal(buf.asBytes())
        val bufferContent = ByteBuf()
        bufferContent.put(signature)
        bufferContent.buffer.put(buf.asBytes())
        return version + Utils.base64Encode(Utils.compress(bufferContent.asBytes()))
    }

    fun getService(serviceType: Short): Service {
        if (serviceType == SERVICE_TYPE_RTC) {
            return ServiceRtc()
        }
        if (serviceType == SERVICE_TYPE_RTM) {
            return ServiceRtm()
        }
        if (serviceType == SERVICE_TYPE_FPA) {
            return ServiceFpa()
        }
        if (serviceType == SERVICE_TYPE_CHAT) {
            return ServiceChat()
        }
        if (serviceType == SERVICE_TYPE_EDUCATION) {
            return ServiceEducation()
        }
        throw IllegalArgumentException(String.format("unknown service type: `%d`", serviceType))
    }

    @get:Throws(Exception::class)
    val sign: ByteArray
        get() {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(ByteBuf().put(issueTs).asBytes(), "HmacSHA256"))
            val signing = mac.doFinal(appCert.toByteArray())
            mac.init(SecretKeySpec(ByteBuf().put(salt).asBytes(), "HmacSHA256"))
            return mac.doFinal(signing)
        }

    fun parse(token: String): Boolean {
        if (version != token.substring(0, Utils.VERSION_LENGTH)) {
            return false
        }
        val data: ByteArray =
            Utils.decompress(Utils.base64Decode(token.substring(Utils.VERSION_LENGTH)))
        val buff = ByteBuf(data)
        val signature: String = buff.readString()
        appId = buff.readString()
        issueTs = buff.readInt()
        expire = buff.readInt()
        salt = buff.readInt()
        val servicesNum: Short = buff.readShort()
        for (i in 0 until servicesNum) {
            val serviceType: Short = buff.readShort()
            val service = getService(serviceType)
            service.unpack(buff)
            services[serviceType] = service
        }
        return true
    }

    open class Service {
        var serviceType: Short = 0
        var privileges: TreeMap<Short, Int> = TreeMap()

        constructor()
        constructor(serviceType: Short) {
            this.serviceType = serviceType
        }

        fun addPrivilegeRtc(privilege: PrivilegeRtc, expire: Int) {
            privileges[privilege.intValue] = expire
        }

        fun addPrivilegeRtm(privilege: PrivilegeRtm, expire: Int) {
            privileges[privilege.intValue] = expire
        }

        fun addPrivilegeFpa(privilege: PrivilegeFpa, expire: Int) {
            privileges[privilege.intValue] = expire
        }

        fun addPrivilegeChat(privilege: PrivilegeChat, expire: Int) {
            privileges[privilege.intValue] = expire
        }

        fun addPrivilegeEducation(privilege: PrivilegeEducation, expire: Int) {
            privileges[privilege.intValue] = expire
        }

        open fun pack(buf: ByteBuf): ByteBuf {
            return buf.put(serviceType).putIntMap(privileges)
        }

        open fun unpack(byteBuf: ByteBuf) {
            privileges = byteBuf.readIntMap()
        }
    }

    class ServiceRtc : Service {
        var channelName: String? = null
        var uid: String? = null

        constructor() : super() {
            this.serviceType = SERVICE_TYPE_RTC
        }

        constructor(channelName: String?, uid: String?) : super(SERVICE_TYPE_RTC) {
            this.channelName = channelName
            this.uid = uid
        }

        override fun pack(buf: ByteBuf): ByteBuf {
            super.pack(buf)
            buf.put(channelName!!)
            buf.put(uid!!)
            return buf
        }

        override fun unpack(byteBuf: ByteBuf) {
            super.unpack(byteBuf)
            channelName = byteBuf.readString()
            uid = byteBuf.readString()
        }
    }


    class ServiceRtm : Service {
        var userId: String? = null

        constructor() : super(SERVICE_TYPE_RTM)

        constructor(userId: String?) : super(SERVICE_TYPE_RTM) {
            this.userId = userId
        }

        override fun pack(buf: ByteBuf): ByteBuf {
            super.pack(buf)
            return buf.put(userId!!)
        }

        override fun unpack(byteBuf: ByteBuf) {
            super.unpack(byteBuf)
            userId = byteBuf.readString()
        }
    }

    class ServiceFpa() : Service() {
        init {
            this.serviceType = SERVICE_TYPE_RTC
        }

        override fun pack(buf: ByteBuf): ByteBuf {
            return super.pack(buf)
        }

        override fun unpack(byteBuf: ByteBuf) {
            super.unpack(byteBuf)
        }
    }


    class ServiceChat : Service {
        var userId: String

        constructor() {
            this.serviceType = SERVICE_TYPE_CHAT
            userId = ""
        }

        constructor(userId: String) {
            this.serviceType = SERVICE_TYPE_CHAT
            this.userId = userId
        }

        override fun pack(buf: ByteBuf): ByteBuf {
            return super.pack(buf).put(userId)
        }

        override fun unpack(byteBuf: ByteBuf) {
            super.unpack(byteBuf)
            userId = byteBuf.readString()
        }
    }

    class ServiceEducation : Service {
        var roomUuid: String
        var userUuid: String
        var role: Short

        constructor() {
            this.serviceType = SERVICE_TYPE_EDUCATION
            roomUuid = ""
            userUuid = ""
            role = -1
        }

        constructor(roomUuid: String, userUuid: String, role: Short) {
            this.serviceType = SERVICE_TYPE_EDUCATION
            this.roomUuid = roomUuid
            this.userUuid = userUuid
            this.role = role
        }

        constructor(userUuid: String) {
            this.serviceType = SERVICE_TYPE_EDUCATION
            roomUuid = ""
            this.userUuid = userUuid
            role = -1
        }

        override fun pack(buf: ByteBuf): ByteBuf {
            return super.pack(buf).put(roomUuid).put(userUuid).put(role)
        }

        override fun unpack(byteBuf: ByteBuf) {
            super.unpack(byteBuf)
            roomUuid = byteBuf.readString()
            userUuid = byteBuf.readString()
            role = byteBuf.readShort()
        }
    }

    companion object {
        const val version = "007"
        const val SERVICE_TYPE_RTC: Short = 1
        const val SERVICE_TYPE_RTM: Short = 2
        const val SERVICE_TYPE_FPA: Short = 4
        const val SERVICE_TYPE_CHAT: Short = 5
        const val SERVICE_TYPE_EDUCATION: Short = 7

        fun getUidStr(uid: Int): String {
            return if (uid == 0) {
                ""
            } else (uid and 0xFFFFFFFFL.toInt()).toString()
        }
    }
}