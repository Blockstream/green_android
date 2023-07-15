package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

@Serializable
data class EncryptedData(private val encryptedData: String, private val iv: String) :
    GreenJson<EncryptedData>() {
    fun getEncryptedData(): ByteArray = Base64.decode(encryptedData)
    fun getIv(): ByteArray = Base64.decode(iv)
    override fun kSerializer() = serializer()

    companion object {
        fun fromByteArray(encryptedData: ByteArray, iv: ByteArray) = EncryptedData(
            Base64.encode(encryptedData),
            Base64.encode(iv)
        )

        fun fromString(jsonString: String): EncryptedData? {
            return try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}