package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.AccountTypeSerializer
import com.blockstream.libwally.Wally
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccount(
    @SerialName("name") private val gdkName: String,
    @SerialName("pointer") val pointer: Long,
    @SerialName("receiving_id") val receivingId: String,
    @SerialName("recovery_pub_key") val recoveryPubKey: String = "",
    @SerialName("recovery_chain_code") val recoveryChainCode: String = "",
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type") val type: AccountType,
    @SerialName("bip44_discovered") val bip44Discovered: Boolean? = null,
) : GAJson<SubAccount>() {

    override fun kSerializer(): KSerializer<SubAccount> {
        return serializer()
    }

    val name: String
        get() = gdkName.ifBlank {
            when (type) {
                AccountType.BIP44_LEGACY,
                AccountType.BIP49_SEGWIT_WRAPPED,
                AccountType.BIP84_SEGWIT -> {
                    val type = if (type == AccountType.BIP84_SEGWIT) "Segwit" else "Legacy"
                    val accountNumber = (pointer / 16) + 1

                    "$type account $accountNumber"
                }
                else -> {
                    ""
                }
            }
        }

    fun nameOrDefault(default: String): String = name.ifBlank { default }

    fun getRecoveryChainCodeAsBytes(): ByteArray? {
        return Wally.hex_to_bytes(recoveryChainCode)
    }

    fun getRecoveryPubKeyAsBytes(): ByteArray? {
        return Wally.hex_to_bytes(recoveryPubKey)
    }
}