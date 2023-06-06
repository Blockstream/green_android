package com.blockstream.common.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import saschpe.kase64.base64DecodedBytes
import saschpe.kase64.base64Encoded

object Base64Serializer : KSerializer<ByteArray?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Base64",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: ByteArray?) {
        if(value != null) {
            encoder.encodeString(value.base64Encoded)
        }else{
            encoder.encodeByte(0)
        }
    }

    override fun deserialize(decoder: Decoder): ByteArray? {
        try{
            return decoder.decodeString().base64DecodedBytes
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }
}



