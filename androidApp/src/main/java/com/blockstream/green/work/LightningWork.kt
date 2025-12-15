package com.blockstream.green.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.blockstream.data.extensions.logException
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.lightning.BreezNotification
import com.blockstream.green.managers.NotificationManagerAndroid
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LightningWork(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    private val firebase: FcmCommon by inject()
    private val notificationManager: NotificationManagerAndroid by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notificationManager.createLightningForegroundServiceNotification(context)
        return ForegroundInfo(id.hashCode(), notification)
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default + logException()) {
            val walletId = inputData.getString(WALLET_ID) ?: ""
            val breezNotification = BreezNotification.fromString(
                inputData.getString(
                    BREEZ_NOTIFICATION
                )
            )

            if (breezNotification != null) {
                firebase.doLightningBackgroundWork(walletId, breezNotification)
                Result.success()
            } else {
                logger.d { "Failed to doWork, no notification data" }
                Result.failure()
            }
        }
    }

    companion object : Loggable() {
        private val TAG = LightningWork::class.java.simpleName

        private const val WALLET_ID = "WALLET_ID"
        private const val BREEZ_NOTIFICATION = "BREEZ_NOTIFICATION"

        fun create(walletId: String, breezNotification: BreezNotification, context: Context) {

            val work = OneTimeWorkRequestBuilder<LightningWork>().addTag(TAG).setInputData(
                workDataOf(
                    WALLET_ID to walletId, BREEZ_NOTIFICATION to breezNotification.toJson()
                )
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName = "$TAG-$walletId", existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE, request = work
            )
        }
    }
}
