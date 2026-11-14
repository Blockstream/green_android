package com.blockstream.compose.models.sheets

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_block_height
import blockstream_green.common.generated.resources.id_confirmed
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_output_index
import blockstream_green.common.generated.resources.id_received
import blockstream_green.common.generated.resources.id_received_on
import blockstream_green.common.generated.resources.id_script_type
import blockstream_green.common.generated.resources.id_status
import blockstream_green.common.generated.resources.id_transaction_id
import blockstream_green.common.generated.resources.id_unconfirmed
import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.utils.formatAuto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class CoinInfoViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    class LocalEvents {
        object ViewInExplorer : Event
    }

    abstract val data: StateFlow<List<Pair<StringHolder, StringHolder>>>
}

class CoinInfoViewModel(
    greenWallet: GreenWallet,
    private val selectedAccountAsset: AccountAsset,
    private val amount: String,
    private val amountFiat: String?,
    private val isConfirmed: Boolean,
    private val txHash: String,
    private val outputIndex: Long,
    private val scriptType: String,
    private val blockHeight: Long?,
    private val isBlinded: Boolean
) : CoinInfoViewModelAbstract(greenWallet = greenWallet, accountAsset = selectedAccountAsset) {
    override fun screenName(): String = "CoinInfo"

    private val _data = MutableStateFlow<List<Pair<StringHolder, StringHolder>>>(emptyList())
    override val data = _data.asStateFlow()

    private var fetchedTransaction: Transaction? = null

    init {
        _data.value = buildData()
        fetchDetails()
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ViewInExplorer -> {
                postSideEffect(SideEffects.OpenBrowser(explorerUrl(fetchedTransaction)))
            }
        }
    }

    private fun fetchDetails() {
        doAsync(
            action = {
                session.accountBackend(selectedAccountAsset.account).getTransaction(id = txHash)
            },
            onSuccess = { transaction ->
                fetchedTransaction = transaction
                _data.value = buildData(transaction)
            },
            // Details are optional; if history lookup fails, keep the base fields only.
            onError = {}
        )
    }

    private fun buildData(transaction: Transaction? = fetchedTransaction): List<Pair<StringHolder, StringHolder>> = buildList {
        val receivedOutput = transaction?.outputs?.firstOrNull { it.ptIdx == outputIndex }
            ?: outputIndex.takeIf { it in 0..Int.MAX_VALUE }?.let {
                transaction?.outputs?.getOrNull(it.toInt())
            }

        add(StringHolder.create(Res.string.id_amount) to StringHolder.create(amountDisplay()))
        add(
            StringHolder.create(Res.string.id_status) to StringHolder.create(
                if (isConfirmed) Res.string.id_confirmed else Res.string.id_unconfirmed
            )
        )
        transaction?.createdAtInstant?.formatAuto()?.also {
            add(StringHolder.create(Res.string.id_received) to StringHolder.create(it))
        }
        receivedOutput?.address?.also {
            add(StringHolder.create(Res.string.id_received_on) to StringHolder.create(it))
        }
        add(StringHolder.create(Res.string.id_transaction_id) to StringHolder.create(txHash))
        add(StringHolder.create(Res.string.id_output_index) to StringHolder.create(outputIndex))
        add(StringHolder.create(Res.string.id_script_type) to StringHolder.create(scriptType.uppercase()))
        blockHeight?.takeIf { it > 0L }?.also {
            add(StringHolder.create(Res.string.id_block_height) to StringHolder.create(it))
        }
        transaction?.memo?.takeIf { it.isNotBlank() }?.also {
            add(StringHolder.create(Res.string.id_note) to StringHolder.create(it))
        }
    }

    private fun amountDisplay(): String = amountFiat?.let { "$amount\n$it" } ?: amount

    private fun explorerUrl(transaction: Transaction? = null): String {
        val blinder = if (selectedAccountAsset.account.isLiquid && isBlinded && transaction != null) {
            "#blinded=${transaction.getUnblindedString()}"
        } else {
            ""
        }
        return "${selectedAccountAsset.account.network.explorerUrl}$txHash$blinder"
    }
}

class CoinInfoViewModelPreview : CoinInfoViewModelAbstract(
    greenWallet = previewWallet(),
    accountAsset = previewAccountAsset()
) {
    override val data: StateFlow<List<Pair<StringHolder, StringHolder>>> = MutableStateFlow(
        listOf(
            StringHolder.create(Res.string.id_amount) to StringHolder.create("0.00022000 BTC\n23.56 USD"),
            StringHolder.create(Res.string.id_status) to StringHolder.create(Res.string.id_confirmed),
            StringHolder.create(Res.string.id_received) to StringHolder.create("Aug 18, 2026"),
            StringHolder.create(Res.string.id_received_on) to StringHolder.create("bc1qexampleaddress"),
            StringHolder.create(Res.string.id_transaction_id) to StringHolder.create("3a5f1e2b4c6d7e8f90123456789abcdef123456789abcdef123456789c8d7a6f"),
            StringHolder.create(Res.string.id_output_index) to StringHolder.create("0"),
            StringHolder.create(Res.string.id_script_type) to StringHolder.create("P2WPKH"),
            StringHolder.create(Res.string.id_block_height) to StringHolder.create("860000"),
            StringHolder.create(Res.string.id_note) to StringHolder.create("Lorem ipsum dolor sit amet, consectetur adipiscing elit."),
        )
    )
}
