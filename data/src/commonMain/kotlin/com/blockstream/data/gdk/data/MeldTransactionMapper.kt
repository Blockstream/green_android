package com.blockstream.data.gdk.data

import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant

fun com.blockstream.data.meld.models.MeldTransaction.toTransaction(account: Account): Transaction? {
    if (status == _root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.CANCELLED.name ||
        status == _root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.DECLINED.name ||
        status == _root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.FAILED.name ||
        status == _root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.ERROR.name ||
        status == _root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.VOIDED.name
    ) {
        return null
    }
    
    val btcAmount = destinationAmount ?: return null
    val address = cryptoDetails?.destinationWalletAddress 
        ?: cryptoDetails?.walletAddress 
        ?: return null
    
    val satoshis = (btcAmount * 100_000_000).roundToLong()
    
    val createdAtMs = try {
        Instant.parse(createdAt).toEpochMilliseconds()
    } catch (e: Exception) {
        Clock.System.now().toEpochMilliseconds()
    }
    val createdAtMicros = createdAtMs * 1000
    
    val feeSatoshis = cryptoDetails?.totalFee?.let { 
        (it * 100_000_000).roundToLong() 
    } ?: 0L

    val txHash = if (status == _root_ide_package_.com.blockstream.data.meld.data.MeldTransactionStatus.SETTLED.name) {
        cryptoDetails?.blockchainTransactionId ?: id
    } else {
        id
    }
    
    val inputOutput = InputOutput(
        address = address,
        addressee = "",
        isOutput = true,
        isRelevant = true,
        isSpent = false,
        pointer = 0,
        ptIdx = 0,
        satoshi = satoshis,
        subaccount = 0,
        subtype = 0
    )
    
    val extrasData = mutableListOf(
        "meld_type" to "pending_purchase",
        "meld_id" to id,
        "meld_status" to status
    )
    
    serviceProvider?.let { extrasData.add("meld_provider" to it) }
    sourceAmount?.let { amount ->
        sourceCurrencyCode?.let { currency ->
            extrasData.add("meld_amount" to "$amount $currency")
        }
    }
    cryptoDetails?.blockchainTransactionId?.let {
        extrasData.add("meld_blockchain_tx" to it)
    }
    
    return Transaction(
        accountInjected = account,
        blockHeight = 0,
        canCpfp = false,
        canRBF = false,
        rbfOptin = false,
        createdAtTs = createdAtMicros,
        inputs = listOf(inputOutput),
        outputs = listOf(inputOutput),
        fee = feeSatoshis,
        feeRate = 0,
        memo = "",
        spvVerified = "",
        txHash = txHash,
        type = "incoming",
        satoshi = mapOf("btc" to satoshis),
        transactionVsize = 0,
        transactionWeight = 0,
        extras = extrasData
    )
}

fun Transaction.isMeldPending(): Boolean {
    return extras?.any { it.first == "meld_type" && it.second == "pending_purchase" } == true
}

fun Transaction.getMeldBlockchainTxId(): String? {
    return extras?.firstOrNull { it.first == "meld_blockchain_tx" }?.second?.takeIf { it.isNotEmpty() }
}