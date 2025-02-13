package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.serializers.HtmlEntitiesSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CountlyWidget(
    @SerialName("_id") val id: String,
    @SerialName("app_id") val appId: String,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("name") val name: String,
    @SerialName("questions") val questions: List<Question>? = null,
    @SerialName("type") val type: SurveyType,
    @SerialName("msg") val msg: Messages,
    @SerialName("appearance") val appearance: Appearance,
    @SerialName("followUpType") val followUpType: FollowUpType? = null,
) : GreenJson<CountlyWidget>() {
    override fun keepJsonElement() = true

    override fun kSerializer() = serializer()

    @Transient
    lateinit var widget : Any

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
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("thanks") val thanks: String,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("mainQuestion") val mainQuestion: String = "",
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("followUpAll") val followUpAll: String = "",
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("followUpPromoter") val followUpPromoter: String = "",
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("followUpPassive") val followUpPassive: String = "",
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("followUpDetractor") val followUpDetractor: String = "",
) : GreenJson<Messages>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class Appearance(
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("followUpInput") val followUpInput: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("notLikely") val notLikely: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("likely") val likely: String? = null,
) : GreenJson<Appearance>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class Question(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("question") val question: String,
    @SerialName("required") val required: Boolean = false,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("followUpInput") val followUpInput: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("notLikely") val notLikely: String? = null,
    @Serializable(with = HtmlEntitiesSerializer::class)
    @SerialName("likely") val likely: String? = null,
) : GreenJson<Question>() {
    override fun kSerializer() = serializer()
}

