package com.blockstream.common.data

import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.serializers.HtmlEntitiesSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Promo(
    val id: String,
    @SerialName("is_visible")
    val isVisible: Boolean = true,
    @SerialName("is_small")
    val isSmall: Boolean = false,
    @Serializable(with = HtmlEntitiesSerializer::class)
    val title: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("title_small")
    val titleSmall: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("title_large")
    val titleLarge: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("text_small")
    val textSmall: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("text_large")
    val textLarge: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("cta_small")
    val ctaSmall: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("cta_large")
    val ctaLarge: String? = null,

    @SerialName("image_small")
    val imageSmall: String? = null,
    @SerialName("image_large")
    val imageLarge: String? = null,

    val link: String? = null,
    val screens: List<String>? = null,
): GreenJson<Promo>(), JavaSerializable{
    override fun kSerializer() = serializer()

    companion object{
        val preview1 = Promo(
            id = "jade_upsell_10",
            title = "Jade Discount",
            titleSmall = "Keep the keys to your bitcoin and LBTC assets safely offline.",
            titleLarge = "Exclusive Jade Wallet Savings—Unlock Yours Now!",
            textSmall = "Upgrade your wallet security today, and enjoy a special discount on your purchase.",
            textLarge = "Don’t leave your wallet security to chance, seize this limited time opportunity to get a discount on Jade.\\nUnlike general devices, Jade is specifically crafted to protect your keys from remote hackers and thieves. Pair it with 2FA-protected accounts for defense against even the most sophisticated attacks.",
            ctaSmall = "Get Discount Now",
            ctaLarge = "Claim Your Offer Now",
        )

        val preview2 = Promo(
            id = "jade_upsell_10",
            titleSmall = "The Ultimate Cyber Defense No Image",
            titleLarge = "Exclusive Jade Wallet Savings—Unlock Yours Now!",
            ctaSmall = "Get Discount Now"
        )

        val preview3 = Promo(
            id = "jade_upsell_10",
            titleSmall = "The Ultimate Cyber Defense 1 2 3 4 5 6 7 8 9 0",
        )

        val preview4 = Promo(
            id = "jade_upsell_10",
            textSmall = "The Ultimate Cyber Defense 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0",
        )
    }
}