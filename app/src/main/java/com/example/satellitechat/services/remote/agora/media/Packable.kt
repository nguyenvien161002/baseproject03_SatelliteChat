package com.example.satellitechat.services.remote.agora.media

interface Packable {
    fun marshal(out: ByteBuf?): ByteBuf?
}