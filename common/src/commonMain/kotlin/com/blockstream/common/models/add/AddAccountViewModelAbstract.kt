package com.blockstream.common.models.add

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_creating_your_s_account
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.domain.account.CreateAccountUseCase
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.sideeffects.SideEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class AddAccountViewModelAbstract(greenWallet: GreenWallet, val assetId: String?, val popTo: PopTo?) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    val createAccountUseCase: CreateAccountUseCase by inject()

    internal val _accountTypeBeingCreated: MutableStateFlow<AccountTypeLook?> = MutableStateFlow(null)
    internal var _pendingSideEffect: SideEffect? = null

    open fun assetId() = assetId

    init {
        _accountTypeBeingCreated.filterNotNull().onEach {
            onProgressDescription.value = getString(Res.string.id_creating_your_s_account, it.accountType.toString())
        }.launchIn(viewModelScope)
    }

    protected fun createAccount(
        accountType: AccountType,
        accountName: String,
        network: Network,
        mnemonic: String? = null,
        xpub: String? = null
    ) {
        _accountTypeBeingCreated.value = AccountTypeLook(accountType)

        doAsync({
            createAccountUseCase(
                session = session,
                wallet = greenWallet,
                accountType = accountType,
                accountName = accountName,
                network = network,
                mnemonic = mnemonic,
                xpub = xpub,
                hwInteraction = this
            )
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {

            val accountAsset = AccountAsset.fromAccountAsset(
                account = it,
                assetId = assetId() ?: it.network.policyAsset,
                session = session
            )

            // or setActiveAccount
            postEvent(Events.SetAccountAsset(accountAsset, setAsActive = true))
            postSideEffect(SideEffects.AccountCreated(accountAsset))

            postSideEffect(SideEffects.NavigateToRoot(popTo = popTo))

            countly.createAccount(session, it)
        })
    }

    protected fun networkForAccountType(accountType: AccountType, asset: EnrichedAsset): Network {
        return when (accountType) {
            AccountType.BIP44_LEGACY,
            AccountType.BIP49_SEGWIT_WRAPPED,
            AccountType.BIP84_SEGWIT,
            AccountType.BIP86_TAPROOT -> {
                when {
                    asset.isBitcoin -> session.bitcoinSinglesig!!
                    asset.isLiquidNetwork(session) -> session.liquidSinglesig!!
                    else -> throw Exception("Network not found")
                }
            }

            AccountType.STANDARD -> when {
                asset.isBitcoin -> session.bitcoinMultisig!!
                asset.isLiquidNetwork(session) -> session.liquidMultisig!!
                else -> throw Exception("Network not found")
            }

            AccountType.AMP_ACCOUNT -> session.liquidMultisig!!
            AccountType.TWO_OF_THREE -> session.bitcoinMultisig!!
            AccountType.LIGHTNING -> session.lightning!!
            AccountType.UNKNOWN -> throw Exception("Network not found")
        }
    }

    companion object : Loggable()
}
