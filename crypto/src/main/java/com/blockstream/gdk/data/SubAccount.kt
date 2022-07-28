package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.AccountTypeSerializer
import com.blockstream.libwally.Wally
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SubAccount(
    @SerialName("name") private val gdkName: String,
    @SerialName("pointer") val pointer: Long,
    @SerialName("hidden") val hidden: Boolean = false,
    @SerialName("receiving_id") val receivingId: String,
    @SerialName("recovery_pub_key") val recoveryPubKey: String = "",
    @SerialName("recovery_chain_code") val recoveryChainCode: String = "",
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type") val type: AccountType,
    @SerialName("bip44_discovered") val bip44Discovered: Boolean? = null,
) : GAJson<SubAccount>(), Parcelable {

    override fun kSerializer() = serializer()

    @IgnoredOnParcel
    val isSinglesig by lazy {
        when (type) {
            AccountType.BIP44_LEGACY, AccountType.BIP49_SEGWIT_WRAPPED, AccountType.BIP84_SEGWIT, AccountType.BIP86_TAPROOT -> {
                true
            }
            else -> false
        }
    }

    @IgnoredOnParcel
    val isMultisig by lazy {
        when (type) {
            AccountType.STANDARD, AccountType.AMP_ACCOUNT, AccountType.TWO_OF_THREE -> {
                true
            }
            else -> false
        }
    }

    private val name: String
        get() = gdkName.ifBlank {
            when (type) {
                AccountType.BIP44_LEGACY,
                AccountType.BIP49_SEGWIT_WRAPPED,
                AccountType.BIP84_SEGWIT,
                AccountType.BIP86_TAPROOT -> {
                    val type = when (type) {
                        AccountType.BIP44_LEGACY -> {
                            "Legacy"
                        }
                        AccountType.BIP49_SEGWIT_WRAPPED -> {
                            "Legacy SegWit"
                        }
                        AccountType.BIP84_SEGWIT -> {
                            "SegWit"
                        }
                        else -> {
                            "Taproot"
                        }
                    }
                    // Only on the #1 accounts, add the account word to help users understand the account based concept
                    if (accountNumber == 1L) {
                        "$type Account $accountNumber"
                    } else {
                        "$type $accountNumber"
                    }
                }
                else -> {
                    ""
                }
            }
        }

    fun nameOrDefault(default: String): String = name.ifBlank { default }

    val accountNumber: Long
        get() = when (type) {
            AccountType.BIP44_LEGACY,
            AccountType.BIP49_SEGWIT_WRAPPED,
            AccountType.BIP84_SEGWIT,
            AccountType.BIP86_TAPROOT -> {
                (pointer / 16) + 1
            }
            else -> {
                pointer + 1
            }
        }

    fun getRecoveryChainCodeAsBytes(): ByteArray? {
        return Wally.hex_to_bytes(recoveryChainCode)
    }

    fun getRecoveryPubKeyAsBytes(): ByteArray? {
        return Wally.hex_to_bytes(recoveryPubKey)
    }
}