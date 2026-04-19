package com.blockstream.domain.walletabi.execution

import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.params.AddressParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.toJsonElement
import com.blockstream.data.transaction.TransactionConfirmation
import com.blockstream.domain.send.GetTransactionConfirmationUseCase

fun interface WalletAbiReviewPreviewer {
    suspend fun prepare(
        session: GdkSession,
        plan: WalletAbiExecutionPlan,
        denomination: Denomination?
    ): WalletAbiPreparedExecution
}

data class WalletAbiPreparedExecution(
    val plan: WalletAbiExecutionPlan,
    val params: CreateTransactionParams,
    val transaction: CreateTransaction,
    val confirmation: TransactionConfirmation
)

class DefaultWalletAbiReviewPreviewer(
    private val getTransactionConfirmationUseCase: GetTransactionConfirmationUseCase =
        GetTransactionConfirmationUseCase()
) : WalletAbiReviewPreviewer {
    override suspend fun prepare(
        session: GdkSession,
        plan: WalletAbiExecutionPlan,
        denomination: Denomination?
    ): WalletAbiPreparedExecution {
        val params = plan.toCreateTransactionParams(session)
        val transaction = session.createTransaction(
            network = plan.selectedAccount.network,
            params = params
        )
        transaction.error?.takeIf { it.isNotBlank() }?.let { error ->
            error(error)
        }

        return WalletAbiPreparedExecution(
            plan = plan,
            params = params,
            transaction = transaction,
            confirmation = getTransactionConfirmationUseCase(
                params = params,
                transaction = transaction,
                account = plan.selectedAccount,
                session = session,
                denomination = denomination,
                isAddressVerificationOnDevice = false
            )
        )
    }
}

internal suspend fun WalletAbiExecutionPlan.toCreateTransactionParams(
    session: GdkSession
): CreateTransactionParams {
    return CreateTransactionParams(
        from = selectedAccount.accountAsset,
        addressees = outputs.map { output ->
            AddressParams(
                address = output.destinationAddress,
                satoshi = output.amountSat,
                assetId = output.assetId.takeIf { selectedAccount.network.isLiquid }
            )
        }.toJsonElement(),
        feeRate = feeRate,
        utxos = session.getUnspentOutputs(selectedAccount).unspentOutputs
    )
}
