package com.blockstream.data.gdk.data

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.serializers.AccountTypeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account constructor(
    private var networkInjected: Network? = null,
    private var policyAsset: EnrichedAsset? = null,
    @SerialName("name")
    private val gdkName: String,
    @SerialName("pointer")
    val pointer: Long,
    @SerialName("hidden")
    val hidden: Boolean = false,
    @SerialName("receiving_id")
    val receivingId: String = "",
    @SerialName("recovery_xpub")
    val recoveryXpub: String? = null,
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type")
    val type: AccountType,
    @SerialName("bip44_discovered")
    val bip44Discovered: Boolean? = null,
    @SerialName("core_descriptors")
    val coreDescriptors: List<String>? = null,
    @SerialName("slip132_extended_pubkey")
    val extendedPubkey: String? = null,
    @SerialName("user_path")
    val derivationPath: List<Long>? = null
) : GreenJson<Account>(), Comparable<Account> {

    override fun kSerializer() = serializer()

    fun setup(session: GdkSession, network: Network) {
        networkInjected = network
        policyAsset = EnrichedAsset.create(session = session, network = network)
    }

    fun balance(session: GdkSession) = session.accountAssets(this).value.policyAsset

    val network
        get() = networkInjected!!

    val id: String by lazy {
        "$networkId:$pointer"
    }

    val isSinglesig
        get() = network.isSinglesig

    val isMultisig
        get() = network.isMultisig

    val isLightning
        get() = type == AccountType.LIGHTNING

    val networkId
        get() = network.id

    val isBitcoin
        get() = network.isBitcoin

    val isBitcoinOrLightning
        get() = network.isBitcoinOrLightning

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

    val countlyId
        get() = network.countlyId

    val accountAsset
        get() = AccountAsset(this, policyAsset!!)

    private val weight by lazy {
        when {
            isBitcoin && isSinglesig -> 0
            isBitcoin && isMultisig -> 1
            isLightning -> 2
            isLiquid && isSinglesig -> 3
            isLiquid && isMultisig && !isAmp -> 4
            isLiquid && isMultisig && isAmp -> 5
            else -> 6
        }
    }

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

    private val accountNumber: Long
        get() = bip32Pointer + 1

    override fun compareTo(other: Account): Int {
        return if (weight == other.weight) {
            pointer.compareTo(other.pointer)
        } else {
            weight.compareTo(other.weight)
        }
    }

    fun isFunded(session: GdkSession): Boolean {
        return session.accountAssets(this).value.assets.values.sum() > 0
    }

    val accountBalance
        get() = AccountBalance.create(this)

    val accountAssetBalance
        get() = AccountAssetBalance.create(this.accountAsset)
}