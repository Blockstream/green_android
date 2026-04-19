package com.blockstream.data.gdk

import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

suspend fun prepareSoftwareTransactionForBroadcast(
    session: GdkSession,
    account: Account,
    transaction: CreateTransaction,
    memo: String? = null
): PreparedSoftwareTransaction {
    var processedTransaction = transaction

    if (account.network.isLiquid && !processedTransaction.isBump() && !processedTransaction.isSweep()) {
        processedTransaction = session.blindTransaction(account.network, processedTransaction)
    }

    processedTransaction = session.signTransaction(account.network, processedTransaction)

    val signedTransaction = JsonObject(
        requireNotNull(processedTransaction.jsonElement) {
            "Signed transaction is missing its JSON payload"
        }.jsonObject.toMutableMap().apply {
            this["memo"] = JsonPrimitive(memo?.takeIf { it.isNotBlank() }?.trim().orEmpty())
        }
    )

    return PreparedSoftwareTransaction(
        transaction = processedTransaction,
        signedTransaction = signedTransaction
    )
}

suspend fun broadcastPreparedSoftwareTransaction(
    session: GdkSession,
    account: Account,
    preparedTransaction: PreparedSoftwareTransaction,
    twoFactorResolver: TwoFactorResolver
): ProcessedTransactionDetails {
    return session.sendTransaction(
        account = account,
        signedTransaction = preparedTransaction.signedTransaction,
        isSendAll = preparedTransaction.transaction.isSendAll,
        isBump = preparedTransaction.transaction.isBump(),
        twoFactorResolver = twoFactorResolver
    )
}

data class PreparedSoftwareTransaction(
    val transaction: CreateTransaction,
    val signedTransaction: JsonElement
)

suspend fun executeSoftwareTransaction(
    session: GdkSession,
    account: Account,
    transaction: CreateTransaction,
    memo: String? = null,
    twoFactorResolver: TwoFactorResolver
): ProcessedTransactionDetails {
    val preparedTransaction = prepareSoftwareTransactionForBroadcast(
        session = session,
        account = account,
        transaction = transaction,
        memo = memo
    )

    return broadcastPreparedSoftwareTransaction(
        session = session,
        account = account,
        preparedTransaction = preparedTransaction,
        twoFactorResolver = twoFactorResolver
    )
}
