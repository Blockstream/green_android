package com.blockstream.data.walletabi.walletconnect

import lwk.WalletAbiWalletConnectSessionInfo
import lwk.WalletAbiWalletConnectSessionProposal
import lwk.WalletAbiWalletConnectSessionRequest
import lwk.WalletAbiWalletConnectRpcErrorKind
import lwk.WalletAbiWalletConnectTransportAction

interface WalletAbiWalletConnectBridgeListener {
    fun onSessionProposal(proposal: WalletAbiWalletConnectSessionProposal)
    fun onSessionRequest(request: WalletAbiWalletConnectSessionRequest)
    fun onSessionDelete(topic: String)
    fun onSessionExtend(session: WalletAbiWalletConnectSessionInfo)
    fun onError(message: String)
}

data class WalletAbiWalletConnectTransportExecutionResult(
    val actionId: String,
    val confirmedSessionInfo: WalletAbiWalletConnectSessionInfo? = null,
)

interface WalletAbiWalletConnectBridge {
    suspend fun initialize()
    fun setListener(listener: WalletAbiWalletConnectBridgeListener?)
    suspend fun pair(uri: String)
    suspend fun rejectProposal(proposalId: ULong, message: String)
    suspend fun respondWalletAbiError(
        topic: String,
        requestId: ULong,
        message: String,
        errorKind: WalletAbiWalletConnectRpcErrorKind,
    )
    suspend fun getActiveSessions(): List<WalletAbiWalletConnectSessionInfo>
    suspend fun getPendingRequests(topic: String): List<WalletAbiWalletConnectSessionRequest>
    suspend fun execute(action: WalletAbiWalletConnectTransportAction): WalletAbiWalletConnectTransportExecutionResult
}

class NoOpWalletAbiWalletConnectBridge : WalletAbiWalletConnectBridge {
    private var listener: WalletAbiWalletConnectBridgeListener? = null

    override suspend fun initialize() = Unit

    override fun setListener(listener: WalletAbiWalletConnectBridgeListener?) {
        this.listener = listener
    }

    override suspend fun pair(uri: String) {
        listener?.onError("WalletConnect Wallet ABI is only available on Android")
    }

    override suspend fun rejectProposal(proposalId: ULong, message: String) {
        listener?.onError("WalletConnect Wallet ABI is only available on Android")
    }

    override suspend fun respondWalletAbiError(
        topic: String,
        requestId: ULong,
        message: String,
        errorKind: WalletAbiWalletConnectRpcErrorKind,
    ) {
        listener?.onError("WalletConnect Wallet ABI is only available on Android")
    }

    override suspend fun getActiveSessions(): List<WalletAbiWalletConnectSessionInfo> = emptyList()

    override suspend fun getPendingRequests(topic: String): List<WalletAbiWalletConnectSessionRequest> = emptyList()

    override suspend fun execute(
        action: WalletAbiWalletConnectTransportAction,
    ): WalletAbiWalletConnectTransportExecutionResult {
        listener?.onError("WalletConnect Wallet ABI is only available on Android")
        return WalletAbiWalletConnectTransportExecutionResult(actionId = action.actionId)
    }
}
