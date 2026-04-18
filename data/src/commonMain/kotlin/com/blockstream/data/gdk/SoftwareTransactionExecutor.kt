package com.blockstream.data.gdk

import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

suspend fun executeSoftwareTransaction(
    session: GdkSession,
    account: Account,
    transaction: CreateTransaction,
    memo: String? = null,
    twoFactorResolver: TwoFactorResolver
): ProcessedTransactionDetails {
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

    return session.sendTransaction(
        account = account,
        signedTransaction = signedTransaction,
        isSendAll = processedTransaction.isSendAll,
        isBump = processedTransaction.isBump(),
        twoFactorResolver = twoFactorResolver
    )
}
