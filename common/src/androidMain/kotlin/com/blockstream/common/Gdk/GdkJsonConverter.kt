package com.blockstream.common.gdk

import com.blockstream.libgreenaddress.GDKJNI

class GdkJsonConverter constructor(private val jsonConverter: JsonConverter): GDKJNI.JSONConverter {
    override fun toJSONObject(jsonString: String?): Any? {
        return jsonConverter.toJSONObject(jsonString)
    }

    override fun toJSONString(jsonObject: Any?): String {
        return jsonConverter.toJSONString(jsonObject)
    }
}