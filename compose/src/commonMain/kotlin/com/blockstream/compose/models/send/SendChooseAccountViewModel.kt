package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_is_above_the_maximum_payment_limit_of_s
import blockstream_green.common.generated.resources.id_insufficient_funds
import blockstream_green.common.generated.resources.id_select_account
import blockstream_green.common.generated.resources.id_send
import com.blockstream.compose.extensions.previewAccountAssetBalance
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.lightning.maxPayableSatoshi
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.send.SendFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

data class AccountState(
    val account: AccountAssetBalance,
    val message: String? = null,
    val isError: Boolean = false
)

abstract class SendChooseAccountViewModelAbstract(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null
) : CreateTransactionViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "SendChooseAccount"

    abstract val accountsState: StateFlow<List<AccountState>>

    abstract fun selectAccount(account: AccountAssetBalance)
}

class SendChooseAccountViewModel(
    greenWallet: GreenWallet,
    private val address: String,
    private val addressType: AddressInputType,
    val asset: EnrichedAsset,
    val accounts: List<AccountAssetBalance>,
    accountAssetOrNull: AccountAsset? = null
) : SendChooseAccountViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    private val _accountsWithMessage: MutableStateFlow<List<AccountState>> = MutableStateFlow(listOf())
    override val accountsState: StateFlow<List<AccountState>> = _accountsWithMessage

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                isCentered = true,
                title = getString(Res.string.id_send),
                subtitle = getString(Res.string.id_select_account),
            )
        }

        doAsync({

            val amount = sendUseCase.getSendAmountUseCase(session = session, input = address)

            _accountsWithMessage.value = accounts.map { accountBalance ->
                val account = accountBalance.accountAsset.account
                val balance = accountBalance.balance(session = session)

                val maxPayable = if (account.network.isLightning) {
                    session.lightningSdkOrNull?.nodeInfoStateFlow?.value?.maxPayableSatoshi()
                } else null

                val (message, isError) = when {
                    amount == null -> null to false
                    maxPayable != null && amount > maxPayable -> {
                        val formatted = formatSatsForError(maxPayable)
                        "id_amount_is_above_the_maximum_payment_limit_of_s|$formatted" to true
                    }
                    balance > amount -> null to false
                    else -> getString(Res.string.id_insufficient_funds) to true
                }

                AccountState(
                    account = accountBalance,
                    message = message,
                    isError = isError,
                )
            }
        })
    }

    override fun selectAccount(account: AccountAssetBalance) {
        doAsync({

            val sendFlow = sendUseCase.getSendFlowUseCase(
                greenWallet = greenWallet,
                session = session,
                address = address,
                asset = asset,
                account = account.accountAsset
            )

            when (sendFlow) {

                is SendFlow.SelectAmount -> {
                    SideEffects.NavigateTo(
                        NavigateDestinations.Send(
                            greenWallet = greenWallet,
                            address = address,
                            addressType = addressType,
                            accountAsset = sendFlow.account,
                        )
                    )
                }

                is SendFlow.SelectLightningAmount -> {
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendLightningAmount(
                            greenWallet = greenWallet,
                            address = address,
                            addressType = addressType,
                            accountAsset = sendFlow.account,
                        )
                    )
                }

                is SendFlow.SendConfirmation -> {
                    session.pendingTransaction = PendingTransaction(
                        params = sendFlow.params,
                        transaction = sendFlow.transaction,
                        segmentation = TransactionSegmentation(
                            transactionType = TransactionType.SEND,
                            addressInputType = addressType,
                            sendAll = false
                        )
                    )

                    SideEffects.NavigateTo(
                        NavigateDestinations.SendConfirm(
                            greenWallet = greenWallet,
                            accountAsset = sendFlow.account,
                            denomination = denomination.value
                        )
                    )
                }

                is SendFlow.SendLightningConfirmation -> {
                    session.pendingTransaction = PendingTransaction(
                        params = sendFlow.params,
                        transaction = sendFlow.transaction,
                        segmentation = TransactionSegmentation(
                            transactionType = TransactionType.SEND,
                            addressInputType = addressType,
                            sendAll = false
                        )
                    )

                    SideEffects.NavigateTo(
                        NavigateDestinations.SendLightningConfirm(
                            greenWallet = greenWallet,
                            accountAsset = sendFlow.account,
                            invoice = sendFlow.invoice,
                            amountSatoshi = sendFlow.params.swap?.toAmount,
                            denomination = denomination.value
                        )
                    )
                }

                is SendFlow.SelectAsset -> {
                    throw Exception("Asset has already been selected")
                }

                is SendFlow.SelectAccount -> {
                    throw Exception("Account has already been selected")
                }
            }
        }, onSuccess = {
            postSideEffect(it)
        })
    }

    private suspend fun formatSatsForError(satoshi: Long): String {
        val sats = satoshi.toAmountLook(
            session = session,
            denomination = Denomination.SATOSHI,
            withUnit = true,
            withGrouping = true,
        ) ?: "$satoshi"
        val fiat = Denomination.fiat(session)?.let {
            satoshi.toAmountLook(
                session = session,
                denomination = it,
                withUnit = true,
                withGrouping = true,
            )
        }
        return if (fiat != null) "$sats ($fiat)" else sats
    }
}

class SendChooseAccountViewModelPreview(greenWallet: GreenWallet) :
    SendChooseAccountViewModelAbstract(greenWallet = greenWallet) {

    override val accountsState: StateFlow<List<AccountState>> = MutableStateFlow(listOf(AccountState(previewAccountAssetBalance())))

    override fun selectAccount(account: AccountAssetBalance) {

    }

    companion object {
        fun preview() = SendChooseAccountViewModelPreview(previewWallet())
    }
}
