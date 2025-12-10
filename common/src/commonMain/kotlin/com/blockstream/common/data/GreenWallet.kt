@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.common.data

import com.blockstream.common.database.wallet.GetWalletsWithCredentialType
import com.blockstream.common.database.wallet.Wallet
import com.blockstream.common.devices.ConnectionType
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.objectId
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
public data class WalletSerializable(
    public val id: String,
    public val name: String,
    public val xpub_hash_id: String,
    public val active_network: String,
    public val active_account: Long,
    public val is_recovery_confirmed: Boolean,
    public val is_testnet: Boolean,
    public val is_hardware: Boolean,
    public val is_lightning: Boolean,
    public val ask_bip39_passphrase: Boolean,
    public val watch_only_username: String?,
    public val device_identifiers: List<DeviceIdentifier>?,
    public val extras: WalletExtras?,
    public val order: Long
)

fun Wallet.toWalletSerializable(): WalletSerializable {
    return WalletSerializable(
        id = id,
        name = name,
        xpub_hash_id = xpub_hash_id,
        active_network = active_network,
        active_account = active_account,
        is_recovery_confirmed = is_recovery_confirmed,
        is_testnet = is_testnet,
        is_hardware = is_hardware,
        is_lightning = is_lightning,
        ask_bip39_passphrase = ask_bip39_passphrase,
        watch_only_username = watch_only_username,
        device_identifiers = device_identifiers,
        extras = extras,
        order = order
    )
}

fun Wallet.toGreenWallet(): GreenWallet {
    return GreenWallet(wallet = this.toWalletSerializable())
}

fun GetWalletsWithCredentialType.toGreenWallet(): GreenWallet {
    val wallet = WalletSerializable(
        id = id,
        name = name,
        xpub_hash_id = xpub_hash_id,
        active_network = active_network,
        active_account = active_account,
        is_recovery_confirmed = is_recovery_confirmed,
        is_testnet = is_testnet,
        is_hardware = is_hardware,
        is_lightning = is_lightning,
        ask_bip39_passphrase = ask_bip39_passphrase,
        watch_only_username = watch_only_username,
        device_identifiers = device_identifiers,
        extras = extras,
        order = order
    )
    return GreenWallet(wallet = wallet)
}

enum class WalletIcon { REGULAR, WATCH_ONLY, TESTNET, BIP39, HARDWARE, LIGHTNING, QR }

@Serializable
data class GreenWallet constructor(
    var wallet: WalletSerializable,
    val ephemeralIdOrNull: Long? = null
) : GreenJson<GreenWallet>() {
    override fun kSerializer() = serializer()

    var id
        get() = wallet.id
        set(value) {
            wallet = wallet.copy(id = value)
        }

    var name
        get() = wallet.name
        set(value) {
            wallet = wallet.copy(name = value)
        }

    var xPubHashId
        get() = wallet.xpub_hash_id
        set(value) {
            wallet = wallet.copy(xpub_hash_id = value)
        }

    var activeNetwork
        get() = wallet.active_network
        set(value) {
            wallet = wallet.copy(active_network = value)
        }

    var activeAccount
        get() = wallet.active_account
        set(value) {
            wallet = wallet.copy(active_account = value)
        }

    var askForBip39Passphrase
        get() = wallet.ask_bip39_passphrase
        set(value) {
            wallet = wallet.copy(ask_bip39_passphrase = value)
        }

    val watchOnlyUsername
        get() = wallet.watch_only_username

    val isWatchOnly
        get() = wallet.watch_only_username != null

    val isWatchOnlyQr
        get() = isWatchOnly && wallet.device_identifiers?.any { it.connectionType == ConnectionType.QR } == true

    val isWatchOnlySingleSig
        get() = isWatchOnly && watchOnlyUsername.isBlank()

    val isWatchOnlyMultisig
        get() = isWatchOnly && watchOnlyUsername.isNotBlank()

    var isRecoveryConfirmed
        get() = wallet.is_recovery_confirmed
        set(value) {
            wallet = wallet.copy(is_recovery_confirmed = value)
        }

    val isTestnet
        get() = wallet.is_testnet

    val isMainnet
        get() = !isTestnet

    val isHardware
        get() = wallet.is_hardware

    @Deprecated("Lightning shortcut is deprecated")
    var isLightning
        get() = wallet.is_lightning
        set(value) {
            wallet = wallet.copy(is_lightning = value)
        }

    var deviceIdentifiers
        get() = wallet.device_identifiers
        set(value) {
            wallet = wallet.copy(device_identifiers = value)
        }

    var extras
        get() = wallet.extras
        set(value) {
            wallet = wallet.copy(extras = value)
        }

    val isEphemeral
        get() = ephemeralIdOrNull != null

    val ephemeralId
        get() = ephemeralIdOrNull ?: 0L

    val isBip39Ephemeral
        get() = isEphemeral && !isHardware

    val ephemeralBip39Name
        get() = "BIP39 #${ephemeralId}"

    val icon
        get() = when {
            isWatchOnly && deviceIdentifiers?.firstOrNull()?.connectionType == ConnectionType.QR -> WalletIcon.QR
            isWatchOnly -> WalletIcon.WATCH_ONLY
            isTestnet -> WalletIcon.TESTNET
            isBip39Ephemeral -> WalletIcon.BIP39
            isHardware -> WalletIcon.HARDWARE
            else -> WalletIcon.REGULAR
        }

    companion object {
        fun createWallet(
            name: String,
            xPubHashId: String = "",
            activeNetwork: String = "",
            activeAccount: Long = 0,
            watchOnlyUsername: String? = null,
            isRecoveryConfirmed: Boolean = true,
            isTestnet: Boolean = false,
            isHardware: Boolean = false,
            deviceIdentifier: List<DeviceIdentifier>? = null,
            extras: WalletExtras? = null
        ): GreenWallet {
            return Wallet(
                id = objectId().toString(),
                name = name,
                xpub_hash_id = xPubHashId,
                active_network = activeNetwork,
                active_account = activeAccount,
                is_recovery_confirmed = isRecoveryConfirmed,
                is_testnet = isTestnet,
                is_hardware = isHardware,
                is_lightning = false,
                ask_bip39_passphrase = false,
                watch_only_username = watchOnlyUsername,
                device_identifiers = deviceIdentifier,
                extras = extras,
                order = 0L
            ).toGreenWallet()
        }

        fun createEphemeralWallet(
            ephemeralId: Long = 0,
            networkId: String,
            name: String? = null,
            isHardware: Boolean = false,
            isTestnet: Boolean = false,
            extras: WalletExtras? = null
        ): GreenWallet {
            return GreenWallet(
                wallet = WalletSerializable(
                    id = objectId().toString(),
                    name = name ?: networkId.replaceFirstChar { n -> n.titlecase() },
                    xpub_hash_id = networkId,
                    active_network = networkId,
                    active_account = 0,
                    is_recovery_confirmed = true,
                    is_testnet = isTestnet,
                    is_hardware = isHardware,
                    is_lightning = false,
                    ask_bip39_passphrase = false,
                    watch_only_username = null,
                    device_identifiers = null,
                    extras = extras,
                    order = 0L
                ),
                ephemeralIdOrNull = ephemeralId
            )
        }
    }
}
