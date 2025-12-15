package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.serializers.AccountTypeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccountParams constructor(
    @SerialName("name")
    val name: String,
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type")
    val type: AccountType,
    @SerialName("recovery_mnemonic")
    val recoveryMnemonic: String? = null,
    @SerialName("recovery_xpub")
    val recoveryXpub: String? = null,
) : GreenJson<SubAccountParams>() {

    override fun kSerializer() = serializer()
}