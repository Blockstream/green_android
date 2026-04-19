package com.blockstream.domain.walletabi.flow

import com.blockstream.data.walletabi.flow.WalletAbiAccountOptionPayload
import com.blockstream.data.walletabi.flow.WalletAbiApprovalTargetPayload
import com.blockstream.data.walletabi.flow.WalletAbiExecutionDetailsPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowReviewPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import com.blockstream.data.walletabi.flow.WalletAbiInputPayload
import com.blockstream.data.walletabi.flow.WalletAbiJadeContextPayload
import com.blockstream.data.walletabi.flow.WalletAbiOutputPayload
import com.blockstream.data.walletabi.flow.WalletAbiParsedRequestPayload
import com.blockstream.data.walletabi.flow.WalletAbiRuntimeParamsPayload
import com.blockstream.data.walletabi.flow.WalletAbiTxCreateRequestPayload
import com.blockstream.domain.walletabi.request.WalletAbiInput
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import com.blockstream.domain.walletabi.request.WalletAbiRuntimeParams
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class WalletAbiFlowSnapshotRepository(
    private val store: WalletAbiFlowSnapshotStore
) {
    suspend fun load(walletId: String): WalletAbiResumeSnapshot? {
        return store.load(walletId)?.toDomainOrClear(walletId)
    }

    fun observe(walletId: String): Flow<WalletAbiResumeSnapshot?> {
        return store.observe(walletId).mapLatest { payload ->
            payload?.toDomainOrClear(walletId)
        }
    }

    suspend fun save(walletId: String, snapshot: WalletAbiResumeSnapshot) {
        store.save(walletId, snapshot.toPayload())
    }

    suspend fun clear(walletId: String) {
        store.clear(walletId)
    }

    private suspend fun WalletAbiFlowSnapshotPayload.toDomainOrClear(walletId: String): WalletAbiResumeSnapshot? {
        return runCatching {
            toDomain()
        }.getOrElse {
            store.clear(walletId)
            null
        }
    }
}

private fun WalletAbiResumeSnapshot.toPayload(): WalletAbiFlowSnapshotPayload {
    return WalletAbiFlowSnapshotPayload(
        review = review.toPayload(),
        phase = phase.name,
        jade = jade?.toPayload()
    )
}

private fun WalletAbiFlowReview.toPayload(): WalletAbiFlowReviewPayload {
    return WalletAbiFlowReviewPayload(
        requestId = requestContext.requestId,
        walletId = requestContext.walletId,
        method = method,
        title = title,
        message = message,
        accounts = accounts.map { it.toPayload() },
        selectedAccountId = selectedAccountId,
        approvalTarget = approvalTarget.toPayload(),
        parsedRequest = parsedRequest?.toPayload(),
        executionDetails = executionDetails?.toPayload()
    )
}

private fun WalletAbiExecutionDetails.toPayload(): WalletAbiExecutionDetailsPayload {
    return WalletAbiExecutionDetailsPayload(
        destinationAddress = destinationAddress,
        amountSat = amountSat,
        assetId = assetId,
        network = network,
        feeRate = feeRate
    )
}

private fun WalletAbiAccountOption.toPayload(): WalletAbiAccountOptionPayload {
    return WalletAbiAccountOptionPayload(
        accountId = accountId,
        name = name
    )
}

private fun WalletAbiApprovalTarget.toPayload(): WalletAbiApprovalTargetPayload {
    return when (this) {
        is WalletAbiApprovalTarget.Jade -> WalletAbiApprovalTargetPayload(
            kind = "jade",
            deviceName = deviceName,
            deviceId = deviceId
        )

        WalletAbiApprovalTarget.Software -> WalletAbiApprovalTargetPayload(
            kind = "software"
        )
    }
}

private fun WalletAbiJadeContext.toPayload(): WalletAbiJadeContextPayload {
    return WalletAbiJadeContextPayload(
        deviceId = deviceId,
        step = step.name,
        message = message,
        retryable = retryable
    )
}

private fun WalletAbiParsedRequest.toPayload(): WalletAbiParsedRequestPayload {
    return when (this) {
        is WalletAbiParsedRequest.TxCreate -> WalletAbiParsedRequestPayload(
            kind = "tx_create",
            txCreate = request.toPayload()
        )
    }
}

private fun WalletAbiTxCreateRequest.toPayload(): WalletAbiTxCreateRequestPayload {
    return WalletAbiTxCreateRequestPayload(
        abiVersion = abiVersion,
        requestId = requestId,
        network = network.wireValue,
        params = params.toPayload(),
        broadcast = broadcast
    )
}

private fun WalletAbiRuntimeParams.toPayload(): WalletAbiRuntimeParamsPayload {
    return WalletAbiRuntimeParamsPayload(
        inputs = inputs.map { it.toPayload() },
        outputs = outputs.map { it.toPayload() },
        feeRateSatKvb = feeRateSatKvb,
        lockTime = lockTime
    )
}

private fun WalletAbiInput.toPayload(): WalletAbiInputPayload {
    return WalletAbiInputPayload(
        id = id,
        utxoSource = utxoSource,
        unblinding = unblinding,
        sequence = sequence,
        issuance = issuance,
        finalizer = finalizer
    )
}

private fun WalletAbiOutput.toPayload(): WalletAbiOutputPayload {
    return WalletAbiOutputPayload(
        id = id,
        amountSat = amountSat,
        lock = lock,
        asset = asset,
        blinder = blinder
    )
}

private fun WalletAbiFlowSnapshotPayload.toDomain(): WalletAbiResumeSnapshot {
    return WalletAbiResumeSnapshot(
        review = review.toDomain(),
        phase = WalletAbiResumePhase.valueOf(phase),
        jade = jade?.toDomain()
    )
}

private fun WalletAbiFlowReviewPayload.toDomain(): WalletAbiFlowReview {
    return WalletAbiFlowReview(
        requestContext = WalletAbiStartRequestContext(
            requestId = requestId,
            walletId = walletId
        ),
        method = method,
        title = title,
        message = message,
        accounts = accounts.map { it.toDomain() },
        selectedAccountId = selectedAccountId,
        approvalTarget = approvalTarget.toDomain(),
        parsedRequest = parsedRequest?.toDomain(),
        executionDetails = executionDetails?.toDomain()
    )
}

private fun WalletAbiExecutionDetailsPayload.toDomain(): WalletAbiExecutionDetails {
    return WalletAbiExecutionDetails(
        destinationAddress = destinationAddress,
        amountSat = amountSat,
        assetId = assetId,
        network = network,
        feeRate = feeRate
    )
}

private fun WalletAbiAccountOptionPayload.toDomain(): WalletAbiAccountOption {
    return WalletAbiAccountOption(
        accountId = accountId,
        name = name
    )
}

private fun WalletAbiApprovalTargetPayload.toDomain(): WalletAbiApprovalTarget {
    return when (kind) {
        "jade" -> WalletAbiApprovalTarget.Jade(
            deviceName = deviceName,
            deviceId = deviceId
        )

        else -> WalletAbiApprovalTarget.Software
    }
}

private fun WalletAbiJadeContextPayload.toDomain(): WalletAbiJadeContext {
    return WalletAbiJadeContext(
        deviceId = deviceId,
        step = WalletAbiJadeStep.valueOf(step),
        message = message,
        retryable = retryable
    )
}

private fun WalletAbiParsedRequestPayload.toDomain(): WalletAbiParsedRequest {
    return when (kind) {
        "tx_create" -> WalletAbiParsedRequest.TxCreate(
            request = requireNotNull(txCreate) { "Missing tx_create payload" }.toDomain()
        )

        else -> error("Unsupported parsed request payload kind: $kind")
    }
}

private fun WalletAbiTxCreateRequestPayload.toDomain(): WalletAbiTxCreateRequest {
    return WalletAbiTxCreateRequest(
        abiVersion = abiVersion,
        requestId = requestId,
        network = WalletAbiNetwork.entries.first { it.wireValue == network },
        params = params.toDomain(),
        broadcast = broadcast
    )
}

private fun WalletAbiRuntimeParamsPayload.toDomain(): WalletAbiRuntimeParams {
    return WalletAbiRuntimeParams(
        inputs = inputs.map { it.toDomain() },
        outputs = outputs.map { it.toDomain() },
        feeRateSatKvb = feeRateSatKvb,
        lockTime = lockTime
    )
}

private fun WalletAbiInputPayload.toDomain(): WalletAbiInput {
    return WalletAbiInput(
        id = id,
        utxoSource = utxoSource,
        unblinding = unblinding,
        sequence = sequence,
        issuance = issuance,
        finalizer = finalizer
    )
}

private fun WalletAbiOutputPayload.toDomain(): WalletAbiOutput {
    return WalletAbiOutput(
        id = id,
        amountSat = amountSat,
        lock = lock,
        asset = asset,
        blinder = blinder
    )
}
