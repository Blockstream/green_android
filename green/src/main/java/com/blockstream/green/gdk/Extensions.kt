package com.blockstream.green.gdk

import com.blockstream.libgreenaddress.KotlinGDK
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

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