package com.blockstream.common.models.sheets

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.toAmountLookOrNa
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

abstract class AssetDetailsViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset? = null
) : GreenViewModel(
    greenWalletOrNull = greenWallet,
    accountAssetOrNull = accountAsset
) {
    abstract val data: StateFlow<List<Pair<String, String>>>
}

class AssetDetailsViewModel(
    assetId: String,
    accountAsset: AccountAsset?,
    greenWallet: GreenWallet
) : AssetDetailsViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override fun screenName(): String = "AssetDetails"


    val _data: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(listOf())
    override val data = _data.asStateFlow()

    init {
        if (session.isConnected) {
            combine(
                session.block(assetId.networkForAsset(session)),
                (accountAsset?.let { session.accountAssets(it.account) }
                    ?: session.walletAssets)
            ) { block, assets ->

                _data.value = buildList {
                    EnrichedAsset.create(session = session, assetId = assetId).also {
                        val isPolicyAsset = it.assetId.isPolicyAsset(session = session)
                        add("id_name" to (it.nameOrNull(session) ?: "id_no_registered_name_for_this"))

                        if (!isPolicyAsset) {
                            add("id_asset_id" to it.assetId)
                        }

                        add("id_block_height" to "${block.height}")

                        add((if(accountAsset == null) "id_total_balance" else "id_account_balance") to assets.balance(assetId).toAmountLookOrNa(
                            session = session,
                            assetId = assetId,
                            withUnit = true
                        ))

                        if (!isPolicyAsset) {
                            add("id_precision" to "${it.precision}")
                            it.ticker(session)?.also {
                                add("id_ticker" to it)
                            }
                            it.entity?.domain?.also {
                                add("id_issuer" to it)
                            }
                        }
                    }
                }


            }.launchIn(viewModelScope.coroutineScope)
        }

        bootstrap()
    }
}

class AssetDetailsViewModelPreview : AssetDetailsViewModelAbstract(accountAsset = previewAccountAsset(),  greenWallet = previewWallet()) {

    override val data: StateFlow<List<Pair<String, String>>> = MutableStateFlow(
        listOf(
            "Name" to "Tether USD",
            "Asset ID" to "Asset ID",
            "Account Balance" to "0.0500000 USDt",
            "Precision" to "8",
            "Ticker" to "USDt",
            "Issuer" to "tether.io",
        )
    )

    companion object {
        fun preview() = AssetDetailsViewModelPreview()
    }
}