package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.data.EnrichedAsset
import com.blockstream.green.databinding.FilterBottomSheetBinding
import com.blockstream.green.extensions.makeItConstant
import com.blockstream.green.gdk.getAssetName
import com.blockstream.green.ui.items.AssetAccountsListItem
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.select.getSelectExtension
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ChooseAssetAccountBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<FilterBottomSheetBinding, AbstractAssetWalletViewModel>() {

    override val screenName = "ChooseAssetAndAccount"

    private lateinit var fastAdapter: FastAdapter<GenericItem>

    override fun inflate(layoutInflater: LayoutInflater) = FilterBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showDivider = false
        binding.showSearch = false
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
    }

    private fun createEnrichedAssets(): List<EnrichedAsset> {
        return (setOfNotNull(
            EnrichedAsset.createOrNull(session.bitcoin?.policyAsset),
            EnrichedAsset.createOrNull(session.liquid?.policyAsset)
        ) + (session.enrichedAssets.values.takeIf { session.liquid != null } ?: emptyList()))
            .sortedWith(session::sortAssets) + listOfNotNull(session.liquid?.let {
            EnrichedAsset( // Any Liquid Asset
                assetId = it.policyAsset,
                weight = -1,
                isAnyLiquidAsset = true
            )
        })
    }

    private fun createModelAdapter(): ModelAdapter<*, *> {
        val assetsAdapter = ModelAdapter<EnrichedAsset, AssetAccountsListItem> { enrichedAsset ->
            AssetAccountsListItem(
                assetPair = enrichedAsset.assetId to 0L,
                session = session,
                enrichedAsset = enrichedAsset,
                listener = object : ChooseAssetAccountListener {
                    override fun createNewAccountClicked(assetId: String) {
                        (requireParentFragment() as? ChooseAssetAccountListener)?.also {
                            it.createNewAccountClicked(assetId)
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
            )
        }

        session.enrichedAssetsFlow.onEach {
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
                item.assetPair.first.getAssetName(session).lowercase().contains(
                    constraint.toString().lowercase()
                )
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
    fun createNewAccountClicked(assetId: String)
}