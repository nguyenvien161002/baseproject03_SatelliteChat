package com.example.satellitechat.services.remote.agora.media

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class ByteBuf {
    var buffer: ByteBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)

    constructor() {}

    constructor(bytes: ByteArray?) {
        buffer = ByteBuffer.wrap(bytes!!).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun asBytes(): ByteArray {
        val out = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(out, 0, out.size)
        return out
    }

    fun put(v: Short): ByteBuf {
        buffer.putShort(v)
        return this
    }

    fun put(v: ByteArray): ByteBuf {
        put(v.size.toShort())
        buffer.put(v)
        return this
    }

    fun put(v: Int): ByteBuf {
        buffer.putInt(v)
        return this
    }

    fun put(v: Long): ByteBuf {
        buffer.putLong(v)
        return this
    }

    fun put(v: String): ByteBuf {
        return put(v.toByteArray())
    }

    fun put(extra: TreeMap<Short, String>): ByteBuf {
        put(extra.size.toShort())
        for ((key, value) in extra.entries) {
            put(key)
            put(value)
        }
        return this
    }

    fun putIntMap(extra: TreeMap<Short, Int>): ByteBuf {
        put(extra.size.toShort())
        for ((key, value) in extra.entries) {
            if (key != null) {
                put(key)
            }
            if (value != null) {
                put(value)
            }
        }
        return this
    }

    fun readShort(): Short {
        return buffer.getShort()
    }

    fun readInt(): Int {
        return buffer.getInt()
    }

    fun readBytes(): ByteArray {
        val length = readShort()
        val bytes = ByteArray(length.toInt())
        buffer.get(bytes)
        return bytes
    }

    fun readString(): String {
        val bytes = readBytes()
        return String(bytes)
    }

    fun readMap(): TreeMap<Short, String> {
        val map = TreeMap<Short, String>()
        val length = readShort()
        for (i in 0 until length) {
            val k = readShort()
            val v = readString()
            map[k] = v
        }
        return map
    }

    fun readIntMap(): TreeMap<Short, Int> {
        val map = TreeMap<Short, Int>()
        val length = readShort()
        for (i in 0 until length) {
            val k = readShort()
            val v = readInt()
            map[k] = v
        }
        return map
    }
}
