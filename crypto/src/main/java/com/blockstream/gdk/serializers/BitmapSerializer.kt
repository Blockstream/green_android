package com.blockstream.gdk.serializers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayOutputStream
import java.lang.Exception


object BitmapSerializer : KSerializer<Bitmap?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Bitmap",
        PrimitiveKind.STRING
    )
    override fun serialize(encoder: Encoder, value: Bitmap?) {
        if(value != null) {
            val baos = ByteArrayOutputStream()
            value.compress(Bitmap.CompressFormat.PNG, 90, baos)
            val b: ByteArray = baos.toByteArray()
            encoder.encodeString(Base64.encodeToString(b, Base64.DEFAULT))
        }else{
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): Bitmap? {
        try{
            val decodedString = Base64.decode(decoder.decodeString(), Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }
}



