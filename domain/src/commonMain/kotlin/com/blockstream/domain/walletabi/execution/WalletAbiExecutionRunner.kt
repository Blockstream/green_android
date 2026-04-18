package com.blockstream.domain.walletabi.execution

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.executeSoftwareTransaction
import com.blockstream.data.gdk.params.AddressParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.toJsonElement

interface WalletAbiExecutionRunner {
    suspend fun execute(
        session: GdkSession,
        plan: WalletAbiExecutionPlan,
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
        plan: WalletAbiExecutionPlan,
        twoFactorResolver: TwoFactorResolver
    ): WalletAbiExecutionResult {
        val transaction = session.createTransaction(
            network = plan.selectedAccount.network,
            params = plan.toCreateTransactionParams(session)
        )
        val result = softwareTransactionExecutor.execute(
            session = session,
            account = plan.selectedAccount,
            transaction = transaction,
            memo = "",
            twoFactorResolver = twoFactorResolver
        )

        return WalletAbiExecutionResult(
            txHash = result.txHash?.takeIf { it.isNotBlank() }
                ?: error("Wallet ABI execution did not return a transaction hash")
        )
    }
}

private suspend fun WalletAbiExecutionPlan.toCreateTransactionParams(
    session: GdkSession
): CreateTransactionParams {
    return CreateTransactionParams(
        from = selectedAccount.accountAsset,
        addressees = listOf(
            AddressParams(
                address = destinationAddress,
                satoshi = amountSat,
                assetId = assetId.takeIf { selectedAccount.network.isLiquid }
            )
        ).toJsonElement(),
        feeRate = feeRate,
        utxos = session.getUnspentOutputs(selectedAccount).unspentOutputs
    )
}
