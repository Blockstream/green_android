package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_insufficient_funds
import blockstream_green.common.generated.resources.id_select_account
import blockstream_green.common.generated.resources.id_send
import com.blockstream.common.AddressInputType
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.PendingTransaction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.domain.send.SendFlow
import com.blockstream.domain.send.SendUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

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
    internal val sendUseCase: SendUseCase by inject()

    private val _accountsWithMessage: MutableStateFlow<List<AccountState>> = MutableStateFlow(listOf())
    override val accountsState: StateFlow<List<AccountState>> = _accountsWithMessage

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_send),
                subtitle = getString(Res.string.id_select_account),
            )
        }
        
        doAsync({

            val amount = sendUseCase.getSendAmountUseCase(session = session, input = address)

            _accountsWithMessage.value = accounts.map {

                val hasFunds = amount == null || it.balance(session = session) > amount

                AccountState(
                    account = it,
                    message = if (hasFunds) null else getString(Res.string.id_insufficient_funds),
                    isError = !hasFunds
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

                is SendFlow.SendConfirmation -> {
                    session.pendingTransaction = PendingTransaction(
                        params = sendFlow.params,
                        transaction = sendFlow.transaction,
                        segmentation = TransactionSegmentation(
                            transactionType = TransactionType.SEND,
                            addressInputType = _addressInputType,
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
