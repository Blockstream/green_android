package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.green.data.serializers.HtmlEntitiesSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.kotlincrypto.hash.md.MD5

@Serializable
data class PromoFile(val url: String, val file: String, val filePath: String) : GreenJson<PromoFile>() {

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun create(id: String, url: String?, type: String, dir: String): PromoFile? {
            return url?.let {
                val file = "${id}_${type}_${
                    MD5().digest(it.encodeToByteArray()).toHexString().substring(0..6)
                }"

                PromoFile(
                    url = it,
                    file = file,
                    filePath = "$dir/$file"
                )
            }
        }
    }

    override fun kSerializer(): KSerializer<PromoFile> = serializer()
}

@Serializable
data class Promo(
    val id: String,
    @SerialName("is_visible")
    val isVisible: Boolean = true,
    @SerialName("is_small")
    val isSmall: Boolean = false,
    @SerialName("layout_small")
    val layoutSmall: Int = 0,
    @SerialName("layout_large")
    val layoutLarge: Int = 0,
    @SerialName("target")
    val target: String? = null,

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
    @SerialName("overline_small")
    val overlineSmall: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("overline_large")
    val overlineLarge: String? = null,
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
    @SerialName("video_large")
    val videoLarge: String? = null,

    val link: String? = null,
    val screens: List<String>? = null,
) : GreenJson<Promo>() {
    override fun kSerializer() = serializer()

    val imageSmallFile by lazy {
        PromoFile.create(id, imageSmall, "imageSmall", CacheDir)
    }

    val imageLargeFile by lazy {
        PromoFile.create(id, imageLarge, "imageLarge", CacheDir)
    }

    val videoLargeFile by lazy {
        PromoFile.create(id, videoLarge, "videoLarge", CacheDir)
    }

    companion object {

        var CacheDir: String = ""

        val preview1 = Promo(
            id = "jade_upsell_10",
            title = "Jade Discount",
            titleSmall = "Keep the keys to your bitcoin and LBTC assets safely offline.",
            titleLarge = "Exclusive Jade Wallet Savings—Unlock Yours Now!",
            textSmall = "Upgrade your wallet security today, and enjoy a special discount on your purchase.",
            textLarge = "Don’t leave your wallet security to chance, seize this limited time opportunity to get a discount on Jade.\\nUnlike general devices, Jade is specifically crafted to protect your keys from remote hackers and thieves. Pair it with 2FA-protected accounts for defense against even the most sophisticated attacks.",
            ctaSmall = "Get Discount Now",
            ctaLarge = "Claim Your Offer Now",
            layoutSmall = 0
        )

        val preview2 = Promo(
            id = "jade_upsell_10",
            imageSmall = "https://blockstream.com/img/jade/jade-page-virtual-secure-element.svg",
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
            title = "Layout 1 : Jade Discount",
            imageSmall = "https://jade.blockstream.com/assets/jade-image-layout2small.png",
            titleSmall = "Layout 1 : Keep the keys to your bitcoin and LBTC assets safely offline.",
            titleLarge = "Exclusive Jade Wallet Savings—Unlock Yours Now!",
            textSmall = "Upgrade your wallet security today, and enjoy a special discount on your purchase.",
            textLarge = "Don’t leave your wallet security to chance, seize this limited time opportunity to get a discount on Jade.\\nUnlike general devices, Jade is specifically crafted to protect your keys from remote hackers and thieves. Pair it with 2FA-protected accounts for defense against even the most sophisticated attacks.",
            ctaSmall = "Get Discount Now",
            ctaLarge = "Claim Your Offer Now",
            layoutSmall = 0
        )

        val preview5 = Promo(
            id = "jade_upsell_10",
            title = "Layout 1 : Jade Discount",
            imageSmall = "https://jade.blockstream.com/assets/jade-image-layout2small.png",
            titleSmall = "Layout 1 : Keep the keys to your bitcoin and LBTC assets safely offline.",
            titleLarge = "Exclusive Jade Wallet Savings—Unlock Yours Now!",
            textSmall = "Upgrade your wallet security today, and enjoy a special discount on your purchase.",
            textLarge = "Don’t leave your wallet security to chance, seize this limited time opportunity to get a discount on Jade.\\nUnlike general devices, Jade is specifically crafted to protect your keys from remote hackers and thieves. Pair it with 2FA-protected accounts for defense against even the most sophisticated attacks.",
            ctaSmall = "Get Discount Now",
            ctaLarge = "Claim Your Offer Now",
            layoutSmall = 1
        )

        val preview6 = Promo(
            id = "jade_upsell_10",
            title = "Layout 2 : Jade Discount",
            overlineSmall = "MEET JADE BLE",
            imageSmall = "https://jade.blockstream.com/assets/jade-image-layout2small.png",
            titleSmall = "Layout 2 : Keep the keys to your bitcoin and LBTC assets safely offline.",
            titleLarge = "Exclusive Jade Wallet Savings—Unlock Yours Now!",
            textSmall = "Upgrade your wallet security today, and enjoy a special discount on your purchase.",
            textLarge = "Don’t leave your wallet security to chance, seize this limited time opportunity to get a discount on Jade.\\nUnlike general devices, Jade is specifically crafted to protect your keys from remote hackers and thieves. Pair it with 2FA-protected accounts for defense against even the most sophisticated attacks.",
            ctaSmall = "Get Discount Now",
            ctaLarge = "Claim Your Offer Now",
            layoutSmall = 2
        )
    }
}