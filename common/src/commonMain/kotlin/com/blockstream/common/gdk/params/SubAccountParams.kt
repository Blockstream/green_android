package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.serializers.AccountTypeSerializer
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