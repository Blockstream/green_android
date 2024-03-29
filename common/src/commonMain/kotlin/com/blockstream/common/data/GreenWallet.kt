package com.blockstream.common.data

import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.arkivanov.essenty.parcelable.CommonParceler
import com.arkivanov.essenty.parcelable.ParcelReader
import com.arkivanov.essenty.parcelable.ParcelWriter
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.parcelable.TypeParceler
import com.arkivanov.essenty.parcelable.readBoolean
import com.arkivanov.essenty.parcelable.readLong
import com.arkivanov.essenty.parcelable.readString
import com.arkivanov.essenty.parcelable.readStringOrNull
import com.arkivanov.essenty.parcelable.writeBoolean
import com.arkivanov.essenty.parcelable.writeLong
import com.arkivanov.essenty.parcelable.writeString
import com.arkivanov.essenty.parcelable.writeStringOrNull
import com.blockstream.common.database.GetWalletsWithCredentialType
import com.blockstream.common.database.Wallet
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.objectId
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
public data class WalletSerializable(
    public val id: String,
    public val name: String,
    public val xpub_hash_id: String,
    public val active_network: String,
    public val active_account: Long,
    public val is_testnet: Boolean,
    public val is_hardware: Boolean,
    public val is_lightning: Boolean,
    public val ask_bip39_passphrase: Boolean,
    public val watch_only_username: String?,
    public val device_identifiers: List<DeviceIdentifier>?,
    public val extras: WalletExtras?,
    public val order: Long
): Parcelable, JavaSerializable

fun Wallet.toWalletSerializable(): WalletSerializable {
    return WalletSerializable(
        id = id,
        name = name,
        xpub_hash_id = xpub_hash_id,
        active_network = active_network,
        active_account,
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
        is_testnet = is_testnet,
        is_hardware = is_hardware,
        is_lightning = is_lightning,
        ask_bip39_passphrase = ask_bip39_passphrase,
        watch_only_username = watch_only_username,
        device_identifiers = device_identifiers,
        extras = extras,
        order = order
    )
    return GreenWallet(wallet = wallet, hasLightningShortcut = credential_type == CredentialType.LIGHTNING_MNEMONIC)
}

enum class WalletIcon { REGULAR, WATCH_ONLY, TESTNET, BIP39, HARDWARE, LIGHTNING }

@Serializable
@Parcelize
@TypeParceler<Wallet, WalletParceler>()
data class GreenWallet constructor(
    var wallet: WalletSerializable,
    val ephemeralIdOrNull: Long? = null,
    val hasLightningShortcut: Boolean = false
) : GreenJson<GreenWallet>(), Parcelable {
    override fun kSerializer() = serializer()

    var id
        get() = wallet.id
        set(value)  {
            wallet = wallet.copy(id = value)
        }

    var name
        get() = wallet.name
        set(value)  {
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

    val isWatchOnlySingleSig
        get() = isWatchOnly && watchOnlyUsername.isBlank()

    val isTestnet
        get() = wallet.is_testnet

    val isMainnet
        get() = !isTestnet

    val isHardware
        get() = wallet.is_hardware

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
        get() = isEphemeral && !isHardware && !isLightning

    val ephemeralBip39Name
        get() = "BIP39 #${ephemeralId}"

    val icon
        get() = when{
            isWatchOnly -> WalletIcon.WATCH_ONLY
            isTestnet -> WalletIcon.TESTNET
            isBip39Ephemeral -> WalletIcon.BIP39
            isHardware -> WalletIcon.HARDWARE
            isLightning -> WalletIcon.LIGHTNING
            else -> WalletIcon.REGULAR
        }

    fun lightningShortcutWallet(): GreenWallet{
        return copy(wallet = wallet.copy(id = "${wallet.id}-lightning-shortcut", is_lightning = true), ephemeralIdOrNull = 0)
    }

    companion object {
        fun createWallet(
            name: String,
            xPubHashId: String = "",
            activeNetwork: String = "",
            activeAccount: Long = 0,
            watchOnlyUsername: String? = null,
            isTestnet: Boolean = false,
            isHardware: Boolean = false,
            extras: WalletExtras? = null
        ): GreenWallet {
            return Wallet(
                id = objectId().toString(),
                name = name,
                xpub_hash_id = xPubHashId,
                active_network = activeNetwork,
                active_account = activeAccount,
                is_testnet = isTestnet,
                is_hardware = isHardware,
                is_lightning = false,
                ask_bip39_passphrase = false,
                watch_only_username = watchOnlyUsername,
                device_identifiers = null,
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

@Suppress("OPT_IN_USAGE", "OPT_IN_OVERRIDE")
internal object WalletParceler : CommonParceler<Wallet> {
    override fun create(reader: ParcelReader): Wallet = Wallet(
        reader.readString(), // id
        reader.readString(), // name
        reader.readString(), // xpub_hash_id
        reader.readString(), // active_network
        reader.readLong(), // active_account
        reader.readBoolean(), // is_testnet
        reader.readBoolean(), // is_hardware
        reader.readBoolean(), // is_lightning
        reader.readBoolean(), // ask_bip39_passphrase
        reader.readStringOrNull(), // watch_only_username
        reader.readStringOrNull()?.toDeviceIdentifierList(), // device_identifiers
        reader.readStringOrNull()?.toWalletExtras(), // extras
        reader.readLong() // order
    )

    override fun Wallet.write(writer: ParcelWriter) {
        writer.writeString(id)
        writer.writeString(name)
        writer.writeString(xpub_hash_id)
        writer.writeString(active_network)
        writer.writeLong(active_account)
        writer.writeBoolean(is_testnet)
        writer.writeBoolean(is_hardware)
        writer.writeBoolean(is_lightning)
        writer.writeBoolean(ask_bip39_passphrase)
        writer.writeStringOrNull(watch_only_username)
        writer.writeStringOrNull(device_identifiers?.toJson())
        writer.writeStringOrNull(extras?.toJson())
        writer.writeLong(order)
    }
}