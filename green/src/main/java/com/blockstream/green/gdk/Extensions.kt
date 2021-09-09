package com.blockstream.green.gdk

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.gdk.data.*
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.libgreenaddress.KotlinGDK
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

fun Transaction.getConfirmationsMax(session: GreenSession): Int {
    return getConfirmations(session.blockHeight).coerceAtMost((if (session.isLiquid) 3 else 7)).toInt()
}

fun AccountType?.titleRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_standard_account
    AccountType.AMP_ACCOUNT -> R.string.id_amp_account
    AccountType.TWO_OF_THREE -> R.string.id_2of3_account
    AccountType.BIP44_LEGACY -> R.string.id_legacy_account
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_legacy_account
    AccountType.BIP84_SEGWIT -> R.string.id_segwit_account
    else -> R.string.id_unknown
}

fun AccountType?.descriptionRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_standard_accounts_allow_you_to
    AccountType.AMP_ACCOUNT -> R.string.id_amp_accounts_are_only_available
    AccountType.TWO_OF_THREE -> R.string.id_a_2of3_account_requires_two_out
    AccountType.BIP44_LEGACY -> R.string.id_legacy_account
    AccountType.BIP49_SEGWIT_WRAPPED -> R.string.id_bip49_accounts_allow_you_to
    AccountType.BIP84_SEGWIT -> R.string.id_bip84_accounts_allow_you_to
    else -> R.string.id_unknown
}

fun String.getAssetIcon(context: Context, session: GreenSession): Drawable {
    if (this == session.network.policyAsset) {
        return ContextCompat.getDrawable(
            context,
            R.drawable.ic_liquid_bitcoin_60
        )!!
    }

    return session.getAssetDrawableOrDefault(this)
}

fun Asset.getIcon(context: Context, session: GreenSession): Drawable = assetId.getAssetIcon(context, session)

fun Network.getIcon(): Int {
    return network.getNetworkIcon()
}

fun Device.getIcon(): Int{
    return when {
        isTrezor -> R.drawable.trezor_device
        isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun com.blockstream.green.devices.Device.getIcon(): Int{
    return when {
        isTrezor -> R.drawable.trezor_device
        isLedger -> R.drawable.ledger_device
        else -> R.drawable.blockstream_jade_device
    }
}

fun String.getNetworkIcon(): Int{
    if (Network.isMainnet(this)) return R.drawable.ic_bitcoin_network_60
    if (Network.isLiquid(this)) return R.drawable.ic_liquid_network_60
    if (Network.isTestnet(this)) return R.drawable.ic_bitcoin_testnet_network_60
    if (Network.isTestnetLiquid(this)) return R.drawable.ic_liquid_testnet_network_60
    return R.drawable.ic_unknown_network_60
}

fun Wallet.getIcon(): Int = network.getNetworkIcon()

fun Throwable.getGDKErrorCode(): Int {
    return try {
        val message = this.message
        val stringCode = message!!.split(" ".toRegex()).toTypedArray()[1]
        val function = message.split(" ".toRegex()).toTypedArray()[2]
        val code = stringCode.toInt()
        // remap gdk connection error
        if (code == KotlinGDK.GA_ERROR && "GA_connect" == function) KotlinGDK.GA_RECONNECT else code
    } catch (e: Exception) {
        KotlinGDK.GA_ERROR
    }
}

fun Throwable.isNotAuthorized() =
    getGDKErrorCode() == KotlinGDK.GA_NOT_AUTHORIZED || message == "id_invalid_pin"

fun Throwable.isConnectionError() =
    message?.contains("failed to connect") == true

// Run mapper on IO, observer in Android Main
@Suppress("UNCHECKED_CAST")
fun <T, R> T.observable(timeout: Long = 0, mapper: (T) -> R): Single<R> =
    Single.just(this)
        .subscribeOn(Schedulers.io())
        .let {
            if(timeout > 0){
                it.timeout(timeout, TimeUnit.SECONDS)
            }else{
                it
            }
        }
        .map(mapper)
        .observeOn(AndroidSchedulers.mainThread())

fun <T> Single<T>.async(mapper: (T) -> T = { it : T -> it }): Single<T> =
    this.subscribeOn(Schedulers.io())
        .map(mapper)
        .observeOn(AndroidSchedulers.mainThread())

fun Completable.async(): Completable =
    this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.async(): Observable<T> =
    this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())