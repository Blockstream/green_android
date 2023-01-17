package com.blockstream.green.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ly.count.android.sdk.ModuleFeedback

@Serializable
data class CountlyWidget(
    @SerialName("_id") val id: String,
    @SerialName("app_id") val appId: String,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("name") val name: String,
    @SerialName("questions") val questions: List<Question>? = null,
    @SerialName("type") val type: SurveyType,
    @SerialName("msg") val msg: Messages,
    @SerialName("appearance") val appearance: Appearance,
    @SerialName("followUpType") val followUpType: FollowUpType? = null,
) : GAJson<CountlyWidget>() {
    override val keepJsonElement: Boolean = true

    override fun kSerializer() = serializer()

    @kotlinx.serialization.Transient
    lateinit var widget : ModuleFeedback.CountlyFeedbackWidget

    val text: Question? get() = questions?.find { it.type == "text" }
    val rating: Question? get() = questions?.find { it.type == "rating" }
}


@Serializable
enum class FollowUpType{
    @SerialName("score") Score, @SerialName("one") One, @SerialName("none") None
}

@Serializable
enum class SurveyType{
    @SerialName("nps") NPS, @SerialName("survey") Survey
}

@Serializable
data class Messages(
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("thanks") val thanks: String,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("mainQuestion") val mainQuestion: String = "",
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("followUpAll") val followUpAll: String = "",
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("followUpPromoter") val followUpPromoter: String = "",
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("followUpPassive") val followUpPassive: String = "",
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("followUpDetractor") val followUpDetractor: String = "",
) : GAJson<Messages>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class Appearance(
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("followUpInput") val followUpInput: String? = null,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("notLikely") val notLikely: String? = null,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("likely") val likely: String? = null,
) : GAJson<Appearance>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class Question(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("question") val question: String,
    @SerialName("required") val required: Boolean = false,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("followUpInput") val followUpInput: String? = null,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("notLikely") val notLikely: String? = null,
    @Serializable(with = StringHtmlSerializer::class)
    @SerialName("likely") val likely: String? = null,
) : GAJson<Question>() {
    override fun kSerializer() = serializer()
}

