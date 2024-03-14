package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.FilterBottomSheetBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.makeItConstant
import com.blockstream.green.ui.items.AssetAccountsListItem
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.select.getSelectExtension
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChooseAssetAccountBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<FilterBottomSheetBinding, GreenViewModel>() {

    override val screenName = "ChooseAssetAndAccount"

    private lateinit var fastAdapter: FastAdapter<GenericItem>

    override fun inflate(layoutInflater: LayoutInflater) = FilterBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showDivider = false
        binding.showSearch = true
        binding.showLoader = true

        // Keep the height of the window always constant
        makeItConstant()

        val modelAdapter = createModelAdapter()

        fastAdapter = FastAdapter.with(listOf(modelAdapter))

        fastAdapter.getSelectExtension().also {
            it.isSelectable = true
            it.multiSelect = false
            it.selectWithItemUpdate = true

            it.selectionListener = object : ISelectionListener<GenericItem> {
                override fun onSelectionChanged(item: GenericItem, selected: Boolean) {
                    if (selected) {
                        countly.assetSelect(session)
                    }
                }
            }
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = AlphaCrossFadeAnimator()
            adapter = fastAdapter
        }

        binding.searchTextInputLayout.endIconCustomMode(R.drawable.ic_baseline_search_24)
        binding.searchInputEditText.addTextChangedListener {
            modelAdapter.filter(it)
        }
    }

    private fun createEnrichedAssets(): List<EnrichedAsset> {
        return (setOfNotNull(
            EnrichedAsset.createOrNull(session = session, session.bitcoin?.policyAsset),
            EnrichedAsset.createOrNull(session = session, session.liquid?.policyAsset),
        ) + session.walletAssets.value.assets.keys.map {
            EnrichedAsset.create(session = session, assetId = it)
        }.toSet() + (session.enrichedAssets.value.takeIf { session.liquid != null }?.map {
            EnrichedAsset.create(session = session, assetId = it.assetId)
        } ?: setOf()) + setOfNotNull(
            EnrichedAsset.createAnyAsset(session = session, isAmp = false),
            EnrichedAsset.createAnyAsset(session = session, isAmp = true)
        )).sortedWith(session::sortEnrichedAssets)
    }

    private fun createModelAdapter(): ModelAdapter<*, *> {
        val assetsAdapter = ModelAdapter<EnrichedAsset, AssetAccountsListItem> { enrichedAsset ->
            AssetAccountsListItem(
                assetPair = enrichedAsset to 0L,
                session = session,
                listener = object : ChooseAssetAccountListener {
                    override fun createNewAccountClicked(asset: EnrichedAsset) {
                        (requireParentFragment() as? ChooseAssetAccountListener)?.also {
                            it.createNewAccountClicked(asset)
                            dismiss()
                        }
                    }

                    override fun accountAssetClicked(accountAsset: AccountAsset) {
                        (requireParentFragment() as? ChooseAssetAccountListener)?.also {
                            it.accountAssetClicked(accountAsset)
                            dismiss()
                            countly.accountSelect(session, accountAsset)
                        }
                    }
                }
            ).also {
                // Expance Bitcoin Asset if wallet is btc only
                it.isSelected = enrichedAsset.assetId == BTC_POLICY_ASSET && !session.hasLiquidAccount
            }
        }

        session.enrichedAssets.onEach {
            lifecycleScope.launch(context = Dispatchers.IO) {
                createEnrichedAssets().toList().also {
                    withContext(context = Dispatchers.Main) {
                        binding.showLoader = false
                        assetsAdapter.set(it)
                    }
                }
            }
        }.launchIn(lifecycleScope)

        // Not used
        assetsAdapter.itemFilter.filterPredicate =
            { item: AssetAccountsListItem, constraint: CharSequence? ->
                item.assetPair.first.name(session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.assetPair.first.ticker(session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true || item.assetPair.first.isAnyAsset
            }

        return assetsAdapter
    }

    companion object {
        fun show(fragmentManager: FragmentManager) {
            showSingle(ChooseAssetAccountBottomSheetDialogFragment(), fragmentManager)
        }
    }
}

interface ChooseAssetAccountListener : AccountAssetListener {
    fun createNewAccountClicked(asset: EnrichedAsset)
}