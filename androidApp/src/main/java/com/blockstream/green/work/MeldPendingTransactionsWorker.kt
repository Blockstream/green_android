package com.blockstream.green.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.blockstream.data.extensions.logException
import com.blockstream.domain.meld.GetPendingMeldTransactions
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MeldPendingTransactionsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {
    
    private val getPendingMeldTransactions: GetPendingMeldTransactions by inject()
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default + logException()) {
            val externalCustomerId = inputData.getString(KEY_EXTERNAL_CUSTOMER_ID) 
                ?: return@withContext Result.success()
            
            getPendingMeldTransactions(
                GetPendingMeldTransactions.Params(
                    externalCustomerId = externalCustomerId
                )
            )
            
            Result.success()
        }
    }
    
    companion object : Loggable() {
        private const val TAG = "MeldPendingTransactionsWorker"
        private const val KEY_EXTERNAL_CUSTOMER_ID = "external_customer_id"
        
        fun trigger(context: Context, externalCustomerId: String) {
            logger.d { "Triggering Meld worker for wallet: $externalCustomerId" }

            val work = OneTimeWorkRequestBuilder<MeldPendingTransactionsWorker>()
                .addTag(TAG)
                .setInputData(
                    workDataOf(KEY_EXTERNAL_CUSTOMER_ID to externalCustomerId)
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    "$TAG-$externalCustomerId", 
                    ExistingWorkPolicy.REPLACE, 
                    work
                )
        }
    }
}