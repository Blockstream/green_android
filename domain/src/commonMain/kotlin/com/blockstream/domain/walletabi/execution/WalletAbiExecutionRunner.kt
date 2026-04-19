package com.blockstream.domain.walletabi.execution

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.executeSoftwareTransaction

interface WalletAbiExecutionRunner {
    suspend fun execute(
        session: GdkSession,
        preparedExecution: WalletAbiPreparedExecution,
        twoFactorResolver: TwoFactorResolver
    ): WalletAbiExecutionResult
}

data class WalletAbiExecutionResult(
    val txHash: String
)

fun interface WalletAbiSoftwareTransactionExecutor {
    suspend fun execute(
        session: GdkSession,
        account: Account,
        transaction: CreateTransaction,
        memo: String,
        twoFactorResolver: TwoFactorResolver
    ): ProcessedTransactionDetails
}

class DefaultWalletAbiExecutionRunner(
    private val softwareTransactionExecutor: WalletAbiSoftwareTransactionExecutor =
        WalletAbiSoftwareTransactionExecutor { session, account, transaction, memo, twoFactorResolver ->
            executeSoftwareTransaction(
                session = session,
                account = account,
                transaction = transaction,
                memo = memo,
                twoFactorResolver = twoFactorResolver
            )
        }
) : WalletAbiExecutionRunner {
    override suspend fun execute(
        session: GdkSession,
        preparedExecution: WalletAbiPreparedExecution,
        twoFactorResolver: TwoFactorResolver
    ): WalletAbiExecutionResult {
        val result = softwareTransactionExecutor.execute(
            session = session,
            account = preparedExecution.plan.selectedAccount,
            transaction = preparedExecution.transaction,
            memo = "",
            twoFactorResolver = twoFactorResolver
        )

        return WalletAbiExecutionResult(
            txHash = result.txHash?.takeIf { it.isNotBlank() }
                ?: error("Wallet ABI execution did not return a transaction hash")
        )
    }
}
