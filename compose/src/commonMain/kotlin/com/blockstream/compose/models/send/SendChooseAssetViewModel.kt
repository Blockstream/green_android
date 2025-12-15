package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_select_asset
import blockstream_green.common.generated.resources.id_send
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.domain.send.SendFlow
import com.blockstream.domain.send.SendUseCase
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class SendChooseAssetViewModelAbstract(
    greenWallet: GreenWallet, accountAssetOrNull: AccountAsset? = null
) : CreateTransactionViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {
    override fun screenName(): String = "SendChooseAsset"

    abstract val assets: List<EnrichedAsset>

    abstract fun selectAsset(asset: EnrichedAsset)
}

class SendChooseAssetViewModel(
    greenWallet: GreenWallet,
    private val address: String,
    private val addressType: AddressInputType,
    override val assets: List<EnrichedAsset>,
    accountAssetOrNull: AccountAsset? = null
) : SendChooseAssetViewModelAbstract(greenWallet = greenWallet, accountAssetOrNull = accountAssetOrNull) {

    internal val sendUseCase: SendUseCase by inject()

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_send),
                subtitle = getString(Res.string.id_select_asset),
            )
        }
    }

    override fun selectAsset(asset: EnrichedAsset) {
        doAsync({
            val sendFlow = sendUseCase.getSendFlowUseCase(
                greenWallet = greenWallet,
                session = session,
                address = address,
                asset = asset
            )

            when (sendFlow) {
                is SendFlow.SelectAccount -> {
                    SideEffects.NavigateTo(
                        NavigateDestinations.SendChooseAccount(
                            greenWallet = greenWallet,
                            address = address,
                            addressType = addressType,
                            asset = sendFlow.asset,
                            accounts = sendFlow.accounts,
                        )
                    )
                }

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

                is SendFlow.SelectAsset -> {
                    throw Exception("Asset has already been selected")
                }
            }

        }, onSuccess = {
            postSideEffect(it)
        })
    }
}

class SendChooseAssetViewModelPreview(greenWallet: GreenWallet) :
    SendChooseAssetViewModelAbstract(greenWallet = greenWallet) {
    override val assets: List<EnrichedAsset>
        get() = listOf(EnrichedAsset.PreviewBTC, EnrichedAsset.PreviewLBTC)

    override fun selectAsset(asset: EnrichedAsset) {

    }

    companion object {
        fun preview() = SendChooseAssetViewModelPreview(previewWallet())
    }
}
