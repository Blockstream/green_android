package com.blockstream.green.utils

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.blockstream.jade.firmware.HardwareQATester
import com.blockstream.common.gdk.data.Notification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/*
 * Emulate different scenarios, useful for QA
 */
class QATester(val context: Context) : com.blockstream.jade.firmware.HardwareQATester {
    val corruptedHardwareMessageSign = MutableLiveData(false)
    val corruptedHardwareTxSign = MutableLiveData(false)
    val corruptedJadeFirmwareHash = MutableLiveData(false)

    val notificationsEvents = MutableSharedFlow<QTNotificationDelay>(extraBufferCapacity = 10)

    override fun getAntiExfilCorruptionForMessageSign(): Boolean {
        return corruptedHardwareMessageSign.value ?: false
    }

    override fun getAntiExfilCorruptionForTxSign(): Boolean {
        return corruptedHardwareTxSign.value ?: false
    }

    override fun getFirmwareCorruption(): Boolean {
        return corruptedJadeFirmwareHash.value ?: false
    }

    @Deprecated("Do not use")
    fun getSessionNotificationInjectorFlow() : Flow<Notification> {
        // Disable it completely
        if(isProductionFlavor){
            return emptyFlow()
        }

        // Delay 5 + i seconds between events
        return notificationsEvents.map {
            delay((5 + it.delay).toDuration(DurationUnit.SECONDS))
            it.notification
        }
    }
}

data class QTNotificationDelay(val notification: Notification, val delay: Long = 0)