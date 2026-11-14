package com.blockstream.data.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class PromoCta(
    val label: String,
    val url: String,
) : GreenJson<PromoCta>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class Promo(
    val id: String,
    val target: String? = null,
    val title: String,
    val description: String,
    val cta: PromoCta,
    val imageUrl: String? = null,
) : GreenJson<Promo>() {
    override fun kSerializer() = serializer()

    fun isValid(): Boolean = id.isNotBlank() &&
        title.isNotBlank() &&
        description.isNotBlank() &&
        cta.label.isNotBlank() &&
        cta.url.isHttpsUrl()

    val hasValidImageUrl: Boolean
        get() = imageUrl?.isHttpsUrl() == true

    companion object {
        val preview1 = Promo(
            id = "jade-core-99-aug26",
            target = "only_sww",
            title = "Meet Jade Core, $99",
            description = "$10 off our newest Jade for app users only.",
            cta = PromoCta(
                label = "Buy Now",
                url = "https://store.blockstream.com/"
            ),
            imageUrl = "https://jade.blockstream.com/assets/jade-image-layout2small.png"
        )
    }
}

private fun String.isHttpsUrl(): Boolean = startsWith("https://") &&
    substringAfter("https://").isNotBlank()
