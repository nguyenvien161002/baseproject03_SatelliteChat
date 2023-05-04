package com.example.satellitechat.services.remote.agora.media

interface PackableEx : Packable {
    fun unmarshal(`in`: ByteBuf?)
}