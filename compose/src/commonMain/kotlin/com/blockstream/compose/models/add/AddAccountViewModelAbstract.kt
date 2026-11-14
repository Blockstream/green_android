package com.blockstream.compose.models.add

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_creating_your_s_account
import blockstream_green.common.generated.resources.id_new_account_created
import com.blockstream.compose.events.Events
import com.blockstream.compose.looks.AccountTypeLook
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.PopTo
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Network
import com.blockstream.domain.account.CreateAccountUseCase
import com.blockstream.utils.Loggable
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
        accountName: String? = null,
        network: Network,
        mnemonic: String? = null,
        xpub: String? = null
    ) {
        _accountTypeBeingCreated.value = AccountTypeLook(accountType)

        var accountCreated = false

        doAsync({
            createAccountUseCase(
                session = session,
                wallet = greenWallet,
                accountType = accountType,
                accountName = accountName,
                network = network,
                mnemonic = mnemonic,
                xpub = xpub,
                hwInteraction = this,
                trackAnalytics = false
            )
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            accountCreated = true

            // Track the account type the user selected in this flow, regardless of whether
            // it was newly created or an existing hidden account was reactivated.
            countly.createAccount(session, it)

            val accountAsset = AccountAsset.fromAccountAsset(
                account = it,
                assetId = assetId() ?: it.network.policyAsset,
                session = session
            )

            // or setActiveAccount
            postEvent(Events.SetAccountAsset(accountAsset))
            postSideEffect(SideEffects.AccountCreated(accountAsset))

            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_new_account_created)))

            postSideEffect(SideEffects.NavigateToRoot(popTo = popTo))
        }, onError = {
            if (accountType == AccountType.LIGHTNING && !accountCreated) {
                countly.enableLightningFailed(session)
            }
            if (appInfo.isDebug) {
                it.printStackTrace()
            }
            postSideEffect(SideEffects.ErrorDialog(error = it, supportData = errorReport(it)))
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

            AccountType.AMP_LEGACY_ACCOUNT -> session.liquidMultisig!!
            AccountType.AMP2_ACCOUNT -> session.liquidAmp2!!
            AccountType.TWO_OF_THREE -> session.bitcoinMultisig!!
            AccountType.LIGHTNING -> session.lightning
            AccountType.UNKNOWN -> throw Exception("Network not found")
        }
    }

    companion object : Loggable()
}
