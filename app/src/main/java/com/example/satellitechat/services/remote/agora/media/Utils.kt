package com.example.satellitechat.services.remote.agora.media

import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Utils {
    const val HMAC_SHA256_LENGTH: Long = 32
    const val VERSION_LENGTH = 3
    const val APP_ID_LENGTH = 32
    @Throws(InvalidKeyException::class, NoSuchAlgorithmException::class)
    fun hmacSign(keyString: String, msg: ByteArray?): ByteArray {
        val keySpec = SecretKeySpec(keyString.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(msg)
    }

    fun pack(packableEx: PackableEx): ByteArray {
        val buffer = ByteBuf()
        packableEx.marshal(buffer)
        return buffer.asBytes()
    }

    fun unpack(data: ByteArray?, packableEx: PackableEx) {
        val buffer = ByteBuf(data)
        packableEx.unmarshal(buffer)
    }

    fun base64Encode(data: ByteArray?): String {
        val encodedBytes = Base64.encodeBase64(data)
        return String(encodedBytes)
    }

    fun base64Decode(data: String): ByteArray {
        return Base64.decodeBase64(data.toByteArray())
    }

    fun crc32(data: String): Int {
        // get bytes from string
        val bytes = data.toByteArray()
        return crc32(bytes)
    }

    fun crc32(bytes: ByteArray?): Int {
        val checksum = CRC32()
        checksum.update(bytes)
        return checksum.value.toInt()
    }

    val timestamp: Int
        get() = (Date().time / 1000).toInt()

    fun randomInt(): Int {
        return SecureRandom().nextInt()
    }

    fun isUUID(uuid: String): Boolean {
        return if (uuid.length != 32) {
            false
        } else uuid.matches("\\p{XDigit}+".toRegex())
    }

    fun compress(data: ByteArray): ByteArray {
        var output: ByteArray
        val deflater = Deflater()
        val bos = ByteArrayOutputStream(data.size)
        try {
            deflater.reset()
            deflater.setInput(data)
            deflater.finish()
            val buf = ByteArray(data.size)
            while (!deflater.finished()) {
                val i = deflater.deflate(buf)
                bos.write(buf, 0, i)
            }
            output = bos.toByteArray()
        } catch (e: Exception) {
            output = data
            e.printStackTrace()
        } finally {
            deflater.end()
        }
        return output
    }

    fun decompress(data: ByteArray): ByteArray {
        val inflater = Inflater()
        val bos = ByteArrayOutputStream(data.size)
        try {
            inflater.setInput(data)
            val buf = ByteArray(data.size)
            while (!inflater.finished()) {
                val i = inflater.inflate(buf)
                bos.write(buf, 0, i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inflater.end()
        }
        return bos.toByteArray()
    }

    fun md5(plainText: String): String {
        var secretBytes: ByteArray? = null
        secretBytes = try {
            MessageDigest.getInstance("md5").digest(
                plainText.toByteArray()
            )
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("No md5 digest！")
        }
        var md5code = BigInteger(1, secretBytes).toString(16)
        for (i in 0 until 32 - md5code.length) {
            md5code = "0$md5code"
        }
        return md5code
    }
}