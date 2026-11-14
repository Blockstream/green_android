package com.blockstream.data.backend.gdk

import com.blockstream.data.backend.AbstractAccountBackend
import com.blockstream.data.comparators.ComparatorAssets
import com.blockstream.data.extensions.toSortedLinkedHashMap
import com.blockstream.data.gdk.AuthHandler
import com.blockstream.data.gdk.BcurResolver
import com.blockstream.data.gdk.GAAuthHandler
import com.blockstream.data.gdk.GASession
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.TwoFactorResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Address
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.GdkAddress
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.PreviousAddresses
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.Transactions
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.device.GdkHardwareWallet
import com.blockstream.data.gdk.params.BalanceParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.PreviousAddressParams
import com.blockstream.data.gdk.params.ReceiveAddressParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.gdk.params.TransactionParams.Companion.TRANSACTIONS_PER_PAGE
import com.blockstream.data.gdk.params.UpdateSubAccountParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement

class GdkAccountBackend constructor(
    val networkBackend: GdkNetworkBackend,
    val gdk: Gdk,
    val gdkHwWallet: GdkHardwareWallet?,
    private val account: Account,
    val gaSession: GASession
) : AbstractAccountBackend() {
    val network: Network
        get() = account.network

    override suspend fun getReceiveAddress(): Address {
        return gdk.getReceiveAddress(
            session = gaSession,
            params = ReceiveAddressParams(account.pointer)
        ).result<GdkAddress>()
    }

    override suspend fun getBalance(
        confirmations: Int
    ): Map<String, Long> {
        return gdk.getBalance(
            session = gaSession,
            details = BalanceParams(
                subaccount = account.pointer,
                confirmations = confirmations
            )
        ).result<Map<String, Long>>().also {
            setAssets(Assets(it.toSortedLinkedHashMap(ComparatorAssets(networkBackend, networkBackend.networkAssetManager, networkBackend))))
        }
    }

    override suspend fun getTransactions(
        params: TransactionParams
    ): Transactions {
        return gdk.getTransactions(
            session = gaSession,
            details = params.copy(subaccount = account.pointer)
        ).result<Transactions>().also {
            it.transactions.onEach { tx ->
                tx.accountInjected = account
                tx.confirmationsMaxInjected = tx.getConfirmationsMax(networkBackend.blockStateFlow.value.height)
            }
        }
    }

    override suspend fun getTransaction(id: String): Transaction? {
        // GDK has no lookup-by-hash, so page through the account's history until the matching
        // transaction is found or a short page signals there are no more.
        var offset = 0
        while (true) {
            val page = getTransactions(TransactionParams(offset = offset, limit = TRANSACTIONS_PER_PAGE))
            page.transactions.firstOrNull { it.txHash == id }?.let { return it }

            if (page.transactions.size < TRANSACTIONS_PER_PAGE) {
                return null
            }
            offset += TRANSACTIONS_PER_PAGE
        }
    }

    suspend fun getPreviousAddresses(lastPointer: Int?): PreviousAddresses =
        gdk.getPreviousAddress(
            gaSession,
            PreviousAddressParams(account.pointer, lastPointer = lastPointer)
        ).result()

    override suspend fun updateAccount(name: String?, hidden: Boolean?) {
        gdk.updateSubAccount(
            gaSession,
            UpdateSubAccountParams(
                subaccount = account.pointer,
                name = name,
                hidden = hidden
            )
        ).resolve().also {
            networkBackend.getAccounts()
        }
    }

    override suspend fun signTransaction(createTransaction: CreateTransaction): CreateTransaction =
        gdk.signTransaction(gaSession, createTransaction = createTransaction.jsonElement!!).result()

    suspend fun sendTransaction(
        signedTransaction: JsonElement,
        twoFactorResolver: TwoFactorResolver
    ): ProcessedTransactionDetails =
        gdk.sendTransaction(gaSession, transaction = signedTransaction)
            .result(twoFactorResolver = twoFactorResolver)

    override suspend fun getUnspentOutputs(
        isBump: Boolean,
        isExpired: Boolean,
        expiredAt: Long?
    ): UnspentOutputs =
        gdk.getUnspentOutputs(
            gaSession, if (isExpired) {
                BalanceParams(
                    subaccount = account.pointer,
                    confirmations = 1,
                    expiredAt = expiredAt
                )
            } else {
                BalanceParams(
                    subaccount = account.pointer,
                    confirmations = if (isBump) 1 else 0
                )
            }
        ).result()

    override suspend fun getOutputDescriptors(): String? = gdk.getSubAccount(
        session = gaSession,
        index = account.pointer
    ).result<Account>().outputDescriptors

    override suspend fun createTransaction(
        params: CreateTransactionParams
    ): CreateTransaction = gdk.createTransaction(gaSession, params).result()

    suspend inline fun <reified T> GAAuthHandler.result(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): T {
        return AuthHandler(
            gaAuthHandler = this,
            network = network,
            gdkHwWallet = gdkHwWallet,
            assetsProvider = networkBackend,
            gdk = gdk,
            getTwoFactorConfig = {
                gdk.getTwoFactorConfig(gaSession)
            }
        ).result(
            twoFactorResolver = twoFactorResolver,
            hardwareWalletResolver = hardwareWalletResolver,
            bcurResolver = bcurResolver
        )
    }

    suspend fun GAAuthHandler.resolve(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): AuthHandler {
        return AuthHandler(
            gaAuthHandler = this,
            network = network,
            gdkHwWallet = gdkHwWallet,
            assetsProvider = networkBackend,
            gdk = gdk,
            getTwoFactorConfig = {
                gdk.getTwoFactorConfig(gaSession)
            }
        ).resolve(
            twoFactorResolver = twoFactorResolver,
            hardwareWalletResolver = hardwareWalletResolver,
            bcurResolver = bcurResolver
        )
    }
}