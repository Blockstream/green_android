package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.data.EnrichedAsset
import com.blockstream.green.gdk.getAssetName
import com.blockstream.green.ui.items.AssetListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class EnrichedAssetsBottomSheetDialogFragment : FilterBottomSheetDialogFragment(), FilterableDataProvider {

    @Suppress("UNCHECKED_CAST")
    internal val viewModel: AbstractWalletViewModel by lazy {
        (requireParentFragment() as AbstractWalletFragment<*>).getWalletViewModel()
    }

    val session
        get() = viewModel.session

    override val withDivider: Boolean = false

    override val withSearch: Boolean = false

    private fun createEnrichedAssets(): List<EnrichedAsset> {
        return (setOfNotNull(
            EnrichedAsset.createOrNull(session.bitcoin?.policyAsset),
            EnrichedAsset.createOrNull(session.liquid?.policyAsset)
        ) + (session.enrichedAssets.values.takeIf { session.liquid != null } ?: emptyList())).sortedWith(session::sortAssets)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showLoader = true
    }

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {

        val assetsAdapter = ModelAdapter<EnrichedAsset, AssetListItem> {
            AssetListItem(
                assetPair = it.assetId to 0L,
                session = session,
                showBalance = false,
                withBottomMargin = true
            )
        }

        session.enrichedAssetsFlow.onEach {
            lifecycleScope.launch(context = Dispatchers.IO) {
                createEnrichedAssets().also {
                    withContext(context = Dispatchers.Main) {
                        binding.showLoader = false
                        assetsAdapter.set(it)
                    }
                }
            }
        }.launchIn(lifecycleScope)

        assetsAdapter.itemFilter.filterPredicate = { item: AssetListItem, constraint: CharSequence? ->
                item.assetPair.first.getAssetName(session).lowercase().contains(
                    constraint.toString().lowercase()
                )
            }

        return assetsAdapter
    }

    override fun getFilterHeaderAdapter(requestCode: Int): GenericFastItemAdapter? {
        return null
    }

    override fun getFilterFooterAdapter(requestCode: Int): GenericFastItemAdapter? {
        return null
    }

    override fun filteredItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        requireParentFragment().also {
            if (it is EnrichedAssetsListener) {
                it.assetClicked((item as AssetListItem).assetPair.first)
            }
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager){
            showSingle(EnrichedAssetsBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putBoolean(WITH_DIVIDER, false)
                    bundle.putBoolean(WITH_SEARCH, false)
                }
            }, fragmentManager)
        }
    }
}

interface EnrichedAssetsListener {
    fun assetClicked(assetId: String)
}