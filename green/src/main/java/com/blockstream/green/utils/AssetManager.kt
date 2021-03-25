package com.blockstream.green.utils

import com.blockstream.gdk.data.Assets
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class AssetManager {
    var isUpToDate = false
        private set

    private val assetsSubject: BehaviorSubject<Assets> = BehaviorSubject.createDefault(Assets())

    fun getAssetsObservable(): Observable<Assets> = assetsSubject.hide()

    fun setCache(assets: Assets) {
        assetsSubject.onNext(assets)
    }

    fun updateAssets(assets: Assets) {
        isUpToDate = true
        assetsSubject.onNext(assets)
    }

    fun getAssets(): Assets = assetsSubject.value!!
}