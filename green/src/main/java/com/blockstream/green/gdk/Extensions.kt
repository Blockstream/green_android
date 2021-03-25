package com.blockstream.green.gdk

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.gdk.data.Asset
import com.blockstream.gdk.data.Network
import com.blockstream.libgreenaddress.KotlinGDK
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

fun AccountType?.intRes(): Int = when (this) {
    AccountType.STANDARD -> R.string.id_standard_account
    AccountType.MANAGED_ASSETS -> R.string.id_managed_assets_account
    AccountType.TWO_OF_THREE -> R.string.id_2of3_account
    else -> R.string.id_unknown
}

fun Asset?.getIcon(context: Context, id: String, session: GreenSession): Drawable {
    if (id == "btc") {
        return ContextCompat.getDrawable(
            context,
            R.drawable.ic_liquid_bitcoin_60
        )!!
    }

    val networkBitmap = session.getAssets().icons[id]

    return if(networkBitmap != null){
        BitmapDrawable(context.resources, networkBitmap)
    }else{
        ContextCompat.getDrawable(context, R.drawable.ic_unknown_network_60)!!
    }
}

fun Network.getIcon(): Int {
    return network.getNetworkIcon()
}

fun String.getNetworkIcon(): Int{
    if (this == "mainnet" || this == "electrum-mainnet") return R.drawable.ic_bitcoin_network_60
    if (this == "liquid" || this == "liquid-electrum-mainnet") return R.drawable.ic_liquid_network_60
    if (this == "testnet" || this == "electrum-testnet") return R.drawable.ic_bitcoin_testnet_network_60
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

// Run mapper on IO, observer in Android Main
@Suppress("UNCHECKED_CAST")
fun <T, R> T.observable(mapper: (T) -> R): Single<R> =
    Single.just(this)
        .subscribeOn(Schedulers.io())
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