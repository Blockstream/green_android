package com.blockstream.common.gdk

import com.blockstream.libgreenaddress.GDK

class GdkJsonConverter constructor(private val jsonConverter: JsonConverter): GDK.JSONConverter {
    override fun toJSONObject(jsonString: String?): Any? {
        return jsonConverter.toJSONObject(jsonString)
    }

    override fun toJSONString(jsonObject: Any?): String {
        return jsonConverter.toJSONString(jsonObject)
    }
}