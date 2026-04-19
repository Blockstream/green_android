package com.blockstream.domain.walletabi.execution

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.PreparedSoftwareTransaction
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.broadcastPreparedSoftwareTransaction
import com.blockstream.data.gdk.prepareSoftwareTransactionForBroadcast

interface WalletAbiExecutionRunner {
    suspend fun prepare(
        session: GdkSession,
        preparedExecution: WalletAbiPreparedExecution
    ): WalletAbiPreparedBroadcast {
        error("Wallet ABI execution runner does not support staged preparation")
    }

    suspend fun broadcast(
        session: GdkSession,
        preparedBroadcast: WalletAbiPreparedBroadcast,
        twoFactorResolver: TwoFactorResolver
    ): WalletAbiExecutionResult {
        error("Wallet ABI execution runner does not support staged broadcast")
    }

    suspend fun execute(
        session: GdkSession,
        preparedExecution: WalletAbiPreparedExecution,
        twoFactorResolver: TwoFactorResolver
    ): WalletAbiExecutionResult {
        return broadcast(
            session = session,
            preparedBroadcast = prepare(
                session = session,
                preparedExecution = preparedExecution
            ),
            twoFactorResolver = twoFactorResolver
        )
    }
}

data class WalletAbiExecutionResult(
    val txHash: String
)

data class WalletAbiPreparedBroadcast(
    val preparedExecution: WalletAbiPreparedExecution,
    val preparedTransaction: PreparedSoftwareTransaction
)

fun interface WalletAbiSoftwareTransactionPreparer {
    suspend fun prepare(
        session: GdkSession,
        account: Account,
        transaction: CreateTransaction,
        memo: String
    ): PreparedSoftwareTransaction
}

fun interface WalletAbiSoftwareTransactionBroadcaster {
    suspend fun broadcast(
        session: GdkSession,
        account: Account,
        preparedTransaction: PreparedSoftwareTransaction,
        twoFactorResolver: TwoFactorResolver
    ): ProcessedTransactionDetails
}

class DefaultWalletAbiExecutionRunner(
    private val softwareTransactionPreparer: WalletAbiSoftwareTransactionPreparer =
        WalletAbiSoftwareTransactionPreparer { session, account, transaction, memo ->
            prepareSoftwareTransactionForBroadcast(
                session = session,
                account = account,
                transaction = transaction,
                memo = memo
            )
        },
    private val softwareTransactionBroadcaster: WalletAbiSoftwareTransactionBroadcaster =
        WalletAbiSoftwareTransactionBroadcaster { session, account, preparedTransaction, twoFactorResolver ->
            broadcastPreparedSoftwareTransaction(
                session = session,
                account = account,
                preparedTransaction = preparedTransaction,
                twoFactorResolver = twoFactorResolver
            )
        }
) : WalletAbiExecutionRunner {
    override suspend fun prepare(
        session: GdkSession,
        preparedExecution: WalletAbiPreparedExecution
    ): WalletAbiPreparedBroadcast {
        val preparedTransaction = softwareTransactionPreparer.prepare(
            session = session,
            account = preparedExecution.plan.selectedAccount,
            transaction = preparedExecution.transaction,
            memo = ""
        )

        return WalletAbiPreparedBroadcast(
            preparedExecution = preparedExecution,
            preparedTransaction = preparedTransaction
        )
    }

    override suspend fun broadcast(
        session: GdkSession,
        preparedBroadcast: WalletAbiPreparedBroadcast,
        twoFactorResolver: TwoFactorResolver
    ): WalletAbiExecutionResult {
        val result = softwareTransactionBroadcaster.broadcast(
            session = session,
            account = preparedBroadcast.preparedExecution.plan.selectedAccount,
            preparedTransaction = preparedBroadcast.preparedTransaction,
            twoFactorResolver = twoFactorResolver
        )

        return WalletAbiExecutionResult(
            txHash = result.txHash?.takeIf { it.isNotBlank() }
                ?: error("Wallet ABI execution did not return a transaction hash")
        )
    }
}
