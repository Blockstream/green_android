package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.serializers.AccountTypeSerializer
import com.blockstream.common.utils.hexToByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Account constructor(
    var networkInjected: Network? = null,
    @SerialName("name") private val gdkName: String,
    @SerialName("pointer") val pointer: Long,
    @SerialName("hidden") val hidden: Boolean = false,
    @SerialName("receiving_id") val receivingId: String = "",
    @SerialName("recovery_pub_key") val recoveryPubKey: String = "",
    @SerialName("recovery_chain_code") val recoveryChainCode: String = "",
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type") val type: AccountType,
    @SerialName("bip44_discovered") val bip44Discovered: Boolean? = null,
    @SerialName("core_descriptors") val coreDescriptors: List<String>? = null,
    @SerialName("slip132_extended_pubkey") val extendedPubkey: String? = null,
    @SerialName("user_path") val derivationPath: List<Long>? = null
) : Parcelable, Comparable<Account> {

    @IgnoredOnParcel
    val network
        get() = networkInjected!!

    @IgnoredOnParcel
    val id: String by lazy {
        "$networkId:$pointer"
    }

    @IgnoredOnParcel
    val isSinglesig
        get() = network.isSinglesig

    @IgnoredOnParcel
    val isMultisig
        get() = network.isMultisig

    val isLightning
        get() = type == AccountType.LIGHTNING

    val networkId
        get() = network.id

    val isBitcoin
        get() = network.isBitcoin

    val isBitcoinMainnet
        get() = network.isBitcoinMainnet

    val isLiquidMainnet
        get() = network.isLiquidMainnet

    val isBitcoinTestnet
        get() = network.isBitcoinTestnet

    val isLiquidTestnet
        get() = network.isLiquidTestnet

    val isLiquid
        get() = network.isLiquid

    val isAmp
        get() = type == AccountType.AMP_ACCOUNT

    val outputDescriptors: String?
        get() = coreDescriptors?.joinToString("\n")

    @IgnoredOnParcel
    private val weight by lazy {
        when{
            isBitcoin && isSinglesig -> 0
            isBitcoin && isMultisig -> 1
            isLightning -> 2
            isLiquid && isSinglesig -> 3
            isLiquid && isMultisig && !isAmp -> 4
            isLiquid && isMultisig && isAmp -> 5
            else -> 6
        }
    }

    @IgnoredOnParcel
    val name: String by lazy {
        gdkName.ifBlank {
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

                AccountType.STANDARD -> {
                    "2FA Protected"
                }

                AccountType.AMP_ACCOUNT -> {
                    "AMP"
                }

                AccountType.TWO_OF_THREE -> {
                    "2of3"
                }

                AccountType.LIGHTNING -> {
                    "Lighning"
                }

                AccountType.UNKNOWN -> {
                    "Unknown"
                }
            }
        }
    }

    private val bip32Pointer: Long
        get() = when (type) {
            AccountType.BIP44_LEGACY,
            AccountType.BIP49_SEGWIT_WRAPPED,
            AccountType.BIP84_SEGWIT,
            AccountType.BIP86_TAPROOT -> {
                (pointer / 16)
            }

            else -> {
                pointer
            }
        }

    val accountNumber: Long
        get() = bip32Pointer + 1

    fun getRecoveryChainCodeAsBytes(): ByteArray {
        return recoveryChainCode.hexToByteArray()
    }

    fun getRecoveryPubKeyAsBytes(): ByteArray {
        return recoveryPubKey.hexToByteArray()
    }

    override fun compareTo(other: Account): Int {
        return if (weight == other.weight) {
            pointer.compareTo(other.pointer)
        } else {
            weight.compareTo(other.weight)
        }
    }
}