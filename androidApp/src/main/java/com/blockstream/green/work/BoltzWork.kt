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
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.tryCatch
import com.blockstream.domain.boltz.BoltzUseCase
import com.blockstream.green.data.json.SimpleJson
import com.blockstream.green.data.notifications.models.BoltzNotificationSimple
import com.blockstream.green.managers.NotificationManagerAndroid
import com.blockstream.green.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BoltzWork(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    private val boltzUseCase: BoltzUseCase by inject()
    private val notificationManager: NotificationManagerAndroid by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notificationManager.createLightningForegroundServiceNotification(context)
        return ForegroundInfo(id.hashCode(), notification)
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default + logException()) {

            val swapId = inputData.getString(SWAP_ID) ?: ""
            val boltzNotificationData: BoltzNotificationSimple? = SimpleJson.fromString(
                inputData.getString(BOLTZ_NOTIFICATION)
            )

            if (boltzNotificationData != null) {
                tryCatch {
                    boltzUseCase.handleSwapEventsUseCase(swapId)
                    Result.success()
                } ?: Result.failure()
            } else {
                logger.d { "Failed to doWork, no notification data" }
                Result.failure()
            }
        }
    }

    companion object : Loggable() {
        private val TAG = BoltzWork::class.java.simpleName

        private const val SWAP_ID = "SWAP_ID"
        private const val BOLTZ_NOTIFICATION = "BOLTZ_NOTIFICATION"

        fun create(boltzNotificationData: BoltzNotificationSimple, context: Context) {

            val work = OneTimeWorkRequestBuilder<BoltzWork>().addTag(TAG).setInputData(
                workDataOf(
                    SWAP_ID to boltzNotificationData.id, BOLTZ_NOTIFICATION to boltzNotificationData.toJson()
                )
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName = "$TAG-${boltzNotificationData.id}",
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                request = work
            )
        }
    }
}
