package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.serializers.AccountTypeSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SubAccountParams constructor(
    @SerialName("name") val name: String,
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type") val type: AccountType,
    @SerialName("recovery_mnemonic") val recoveryMnemonic: String? = null,
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,
) : GAJson<SubAccountParams>() {

    override fun kSerializer(): KSerializer<SubAccountParams> {
        return serializer()
    }
}