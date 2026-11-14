package com.blockstream.data.data

import com.blockstream.data.gdk.JsonConverter
import com.blockstream.utils.Loggable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

fun parsePromosV2(jsonArray: JsonArray): List<Promo> {
    val seenIds = mutableSetOf<String>()

    return jsonArray
        .mapNotNull { element ->
            runCatching {
                JsonConverter.JsonDeserializer.decodeFromJsonElement<Promo>(element)
            }.onFailure {
                PromoParser.logger.w { "Skipping malformed promo element: ${it.message}" }
            }.getOrNull()?.let { promo ->
                promo.takeIf { it.isValid() } ?: run {
                    PromoParser.logger.w { "Skipping invalid promo '${promo.id}'" }
                    null
                }
            }
        }
        .filter { promo ->
            if (seenIds.add(promo.id)) {
                true
            } else {
                PromoParser.logger.w { "Skipping duplicate promo id '${promo.id}'" }
                false
            }
        }
}

private object PromoParser : Loggable()
