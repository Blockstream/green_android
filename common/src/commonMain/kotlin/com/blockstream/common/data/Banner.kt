package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.serializers.HtmlEntitiesSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Banner(
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("title")
    val title: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("message")
    val message: String? = null,
    @SerialName("dismissable")
    val dismissable: Boolean? = null,
    @SerialName("is_warning")
    val isWarning: Boolean = false,
    @SerialName("link")
    val link: String? = null,
    @SerialName("screens")
    val screens: List<String>? = null,
    @SerialName("networks")
    val networks: List<String>? = null,
) : GreenJson<Banner>() {
    override fun kSerializer() = serializer()

    val hasNetworks: Boolean
        get() = networks != null

    companion object {
        val preview1 = Banner(
            title = "Lorem Ipsum",
            message = "Lorem ipsum dolor",
            dismissable = true
        )

        val preview2 = Banner(
            title = "Lorem Ipsum &amp;&amp;",
            message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            isWarning = true
        )

        val preview3 = Banner(
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut eget leo sed urna dapibus volutpat et egestas nisl. Curabitur convallis mattis est ac rutrum. Ut at tincidunt felis, sed tincidunt purus. Sed venenatis erat quis neque gravida placerat. Vivamus a tristique quam, ut dictum lorem. Fusce finibus sollicitudin diam, at rutrum eros tincidunt eu. Suspendisse vulputate ex velit, at suscipit sapien eleifend vitae. Nam venenatis dictum nunc ac dignissim. Aliquam vel tristique velit.",
            dismissable = true,
            isWarning = true,
            link = "https://blockstream.com/"
        )
    }
}