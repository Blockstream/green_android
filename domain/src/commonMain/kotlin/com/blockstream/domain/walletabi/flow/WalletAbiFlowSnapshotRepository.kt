package com.blockstream.domain.walletabi.flow

import com.blockstream.data.walletabi.flow.WalletAbiAccountOptionPayload
import com.blockstream.data.walletabi.flow.WalletAbiApprovalTargetPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowReviewPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotPayload
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import com.blockstream.data.walletabi.flow.WalletAbiJadeContextPayload

class WalletAbiFlowSnapshotRepository(
    private val store: WalletAbiFlowSnapshotStore
) {
    suspend fun load(walletId: String): WalletAbiResumeSnapshot? {
        return store.load(walletId)?.toDomain()
    }

    suspend fun save(walletId: String, snapshot: WalletAbiResumeSnapshot) {
        store.save(walletId, snapshot.toPayload())
    }

    suspend fun clear(walletId: String) {
        store.clear(walletId)
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
        title = title,
        message = message,
        accounts = accounts.map { it.toPayload() },
        selectedAccountId = selectedAccountId,
        approvalTarget = approvalTarget.toPayload()
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
        title = title,
        message = message,
        accounts = accounts.map { it.toDomain() },
        selectedAccountId = selectedAccountId,
        approvalTarget = approvalTarget.toDomain()
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
