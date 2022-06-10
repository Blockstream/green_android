package com.blockstream.green.utils

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.AssetQATester
import com.blockstream.gdk.data.Notification
import com.greenaddress.greenapi.HardwareQATester
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/*
 * Emulate different scenarios, useful for QA
 */
class QATester(val context: Context) : HardwareQATester, AssetQATester {
    val corruptedHardwareMessageSign = MutableLiveData(false)
    val corruptedHardwareTxSign = MutableLiveData(false)
    val corruptedJadeFirmwareHash = MutableLiveData(false)

    val assetsFetchDisabled = MutableLiveData(false)
    val assetsIconsFetchDisabled = MutableLiveData(false)
    val assetsGdkCacheDisabled = MutableLiveData(false)

    val notificationsEvents = PublishSubject.create<QTNotificationDelay>()

    override fun getAntiExfilCorruptionForMessageSign(): Boolean {
        return corruptedHardwareMessageSign.value ?: false
    }

    override fun getAntiExfilCorruptionForTxSign(): Boolean {
        return corruptedHardwareTxSign.value ?: false
    }

    override fun getFirmwareCorruption(): Boolean {
        return corruptedJadeFirmwareHash.value ?: false
    }

    override fun isAssetGdkCacheDisabled(): Boolean {
        return assetsGdkCacheDisabled.value ?: false
    }

    override fun isAssetFetchDisabled(): Boolean {
        return assetsFetchDisabled.value ?: false
    }

    override fun isAssetIconsFetchDisabled(): Boolean {
        return assetsIconsFetchDisabled.value ?: false
    }

    fun getSessionNotificationInjectorObservable() : Observable<Notification> {

        // Disable it completely
        if(isProductionFlavor){
            return Observable.empty()
        }

        // Delay 5 + i seconds between events
        return notificationsEvents.delay { item ->
            Observable.timer(
                7 + item.delay,
                TimeUnit.SECONDS
            )
        }.map { it.notification }

    }
}

data class QTNotificationDelay(val notification: Notification, val delay: Long = 0)