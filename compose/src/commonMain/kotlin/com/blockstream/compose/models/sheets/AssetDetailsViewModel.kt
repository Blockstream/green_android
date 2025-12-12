package com.blockstream.compose.models.sheets

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_balance
import blockstream_green.common.generated.resources.id_asset_id
import blockstream_green.common.generated.resources.id_block_height
import blockstream_green.common.generated.resources.id_issuer
import blockstream_green.common.generated.resources.id_name
import blockstream_green.common.generated.resources.id_no_registered_name_for_this
import blockstream_green.common.generated.resources.id_precision
import blockstream_green.common.generated.resources.id_ticker
import blockstream_green.common.generated.resources.id_total_balance
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.toAmountLookOrNa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

abstract class AssetDetailsViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset? = null
) : GreenViewModel(
    greenWalletOrNull = greenWallet,
    accountAssetOrNull = accountAsset
) {
    abstract val data: StateFlow<List<Pair<StringHolder, StringHolder>>>
}

class AssetDetailsViewModel(
    greenWallet: GreenWallet,
    assetId: String,
    accountAsset: AccountAsset?
) : AssetDetailsViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override fun screenName(): String = "AssetDetails"

    private val _data: MutableStateFlow<List<Pair<StringHolder, StringHolder>>> = MutableStateFlow(listOf())
    override val data = _data.asStateFlow()

    init {
        if (session.isConnected) {
            combine(
                session.block(assetId.networkForAsset(session) ?: session.defaultNetwork),
                (accountAsset?.let { session.accountAssets(it.account) }
                    ?: session.walletAssets.map { it.data() }.filterNotNull())
            ) { block, assets ->

                _data.value = buildList {
                    EnrichedAsset.create(session = session, assetId = assetId).also {
                        val isPolicyAsset = it.assetId.isPolicyAsset(session = session)
                        add(
                            StringHolder(stringResource = Res.string.id_name)
                                    to (it.nameOrNull(session) ?: StringHolder(stringResource = Res.string.id_no_registered_name_for_this))
                        )

                        if (!isPolicyAsset) {
                            add(StringHolder.create(Res.string.id_asset_id) to StringHolder.create(it.assetId))
                        }

                        add(StringHolder.create(Res.string.id_block_height) to StringHolder.create(block.height))

                        add(
                            (if (accountAsset == null) StringHolder.create(Res.string.id_total_balance) else StringHolder.create(
                                Res.string.id_account_balance
                            )) to StringHolder.create(
                                assets.balance(assetId).toAmountLookOrNa(
                                    session = session,
                                    assetId = assetId,
                                    withUnit = true
                                )
                            )
                        )

                        if (!isPolicyAsset) {
                            add(StringHolder.create(Res.string.id_precision) to StringHolder.create(it.precision))
                            it.ticker(session)?.also {
                                add(StringHolder.create(Res.string.id_ticker) to StringHolder.create(it))
                            }
                            it.entity?.domain?.also {
                                add(StringHolder.create(Res.string.id_issuer) to StringHolder.create(it))
                            }
                        }
                    }
                }

            }.launchIn(viewModelScope)
        }

        bootstrap()
    }
}

class AssetDetailsViewModelPreview : AssetDetailsViewModelAbstract(accountAsset = previewAccountAsset(), greenWallet = previewWallet()) {

    override val data: StateFlow<List<Pair<StringHolder, StringHolder>>> = MutableStateFlow(
        listOf(
            StringHolder.create("Name") to StringHolder.create("Tether USD"),
            StringHolder.create("Asset ID") to StringHolder.create("Asset ID"),
            StringHolder.create("Account Balance") to StringHolder.create("0.0500000 USDt"),
            StringHolder.create("Precision") to StringHolder.create("8"),
            StringHolder.create("Ticker") to StringHolder.create("USDt"),
            StringHolder.create("Issuer") to StringHolder.create("tether.io"),
        )
    )

    companion object {
        fun preview() = AssetDetailsViewModelPreview()
    }
}