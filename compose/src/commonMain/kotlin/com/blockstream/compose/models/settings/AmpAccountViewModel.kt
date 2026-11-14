package com.blockstream.compose.models.settings

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Network
import com.blockstream.domain.account.CreateAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

abstract class AmpAccountViewModelAbstract(greenWallet: GreenWallet) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "AmpAccount"

    abstract val creatingAccountTypes: StateFlow<Set<AccountType>>
    val canCreateAmp: Boolean get() = session.isTestnet && session.liquidAmp2 != null
    val canCreateLegacy: Boolean get() = !session.isWatchOnlyValue && session.liquidMultisig != null
    val canCreateAccounts: Boolean get() = canCreateAmp || canCreateLegacy

    abstract fun createAmpAccounts(accountTypes: List<AccountType>? = null, actionConfirmed: Boolean = false)
    abstract fun copyAmpAccountId(account: Account)
}

class AmpAccountViewModel(greenWallet: GreenWallet) : AmpAccountViewModelAbstract(greenWallet = greenWallet) {

    class LocalSideEffects {
        data class JadeWoDisableDialog(val accountTypes: List<AccountType>?) : SideEffect
    }

    private val createAccountUseCase: CreateAccountUseCase by inject()

    private val _creatingAccountTypes = MutableStateFlow<Set<AccountType>>(emptySet())
    override val creatingAccountTypes = _creatingAccountTypes.asStateFlow()


    init {
        bootstrap()
    }

    override fun createAmpAccounts(accountTypes: List<AccountType>?, actionConfirmed: Boolean) {
        viewModelScope.launch {
            createAmpAccounts(accountTypes = accountTypes?.takeIf { it.isNotEmpty() } ?: ampAccountTypesForPrimaryCreate(),
                actionConfirmed = actionConfirmed)
        }
    }

    override fun copyAmpAccountId(account: Account) {
        postSideEffect(SideEffects.CopyToClipboard(account.receivingId))
    }

    private suspend fun createAmpAccounts(accountTypes: List<AccountType>, actionConfirmed: Boolean) {
        if (!actionConfirmed && database.getLoginCredential(
                id = greenWallet.id,
                credentialType = CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
            ) != null
        ) {
            // Show Jade WO Disable dialog
            postSideEffect(LocalSideEffects.JadeWoDisableDialog(accountTypes = accountTypes))

            return
        }

        val existingAmpAccountTypes = session.accounts.value
            .filter { it.type.isAmpOrLecacy() }
            .map { it.type }
            .toSet()

        val accountTypesToCreate = accountTypes
            .filter { it.isAmpOrLecacy() }
            .filterNot { it in existingAmpAccountTypes }
            .distinct()

        if (accountTypesToCreate.isEmpty()) return

        val accountNetworksToCreate = accountTypesToCreate.mapNotNull { accountType ->
            networkForAmpAccountType(accountType)?.let { accountType to it }
        }

        if (accountNetworksToCreate.isEmpty()) return

        doAsync({
            val results = accountNetworksToCreate.map { (accountType, network) ->
                runCatching {
                    createAccountUseCase(
                        session = session,
                        wallet = greenWallet,
                        accountType = accountType,
                        network = network,
                        hwInteraction = this@AmpAccountViewModel
                    )
                }
            }

            val pairedResults = accountNetworksToCreate.map { it.first }.zip(results)
            val failedResult = pairedResults.firstOrNull { (type, result) ->
                type == AccountType.AMP2_ACCOUNT && result.isFailure
            }?.second ?: results.takeIf { it.all { r -> r.isFailure } }?.first()

            failedResult?.exceptionOrNull()?.let { throw it }
        }, preAction = {
            _creatingAccountTypes.value = accountNetworksToCreate.map { it.first }.toSet()
        }, postAction = {
            _creatingAccountTypes.value = emptySet()
        })
    }

    private fun ampAccountTypesForPrimaryCreate(): List<AccountType> = buildList {
        when {
            canCreateAmp -> add(AccountType.AMP2_ACCOUNT)
            canCreateLegacy -> add(AccountType.AMP_LEGACY_ACCOUNT)
        }
    }

    private fun networkForAmpAccountType(accountType: AccountType): Network? =
        when (accountType) {
            AccountType.AMP2_ACCOUNT -> session.liquidAmp2
            AccountType.AMP_LEGACY_ACCOUNT -> session.liquidMultisig
            else -> throw IllegalArgumentException("Unsupported AMP account type: $accountType")
        }
}
