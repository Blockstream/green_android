// Bodies of the Zendesk Uploads and Requests API calls that file a support ticket.

package com.blockstream.data.zendesk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateRequestBody(val request: SupportRequest)

@Serializable
internal data class SupportRequest(
    val requester: Requester,
    val subject: String,
    val comment: Comment,
    @SerialName("custom_fields")
    val customFields: List<CustomField>
)

@Serializable
internal data class Requester(val email: String)

@Serializable
internal data class Comment(val body: String, val uploads: List<String>)

@Serializable
internal data class CustomField(val id: Long, val value: String)

@Serializable
internal data class CreatedRequestResponse(
    val request: CreatedRequest? = null,
    // Zendesk can hold an anonymous request as a suspended ticket for an agent to recover
    @SerialName("suspended_ticket")
    val suspendedTicket: CreatedRequest? = null
)

@Serializable
internal data class CreatedRequest(val id: Long)

@Serializable
internal data class UploadResponse(val upload: Upload)

@Serializable
internal data class Upload(val token: String)
