package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.extensions.getAssetName
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.items.AssetListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnrichedAssetsBottomSheetDialogFragment : FilterBottomSheetDialogFragment(), FilterableDataProvider {

    val session: GdkSession
        get() = (requireParentFragment() as? AbstractWalletFragment<*>)?.getWalletViewModel()?.session ?: (requireParentFragment() as AppFragment<*>).getGreenViewModel()!!.session

    override val withDivider: Boolean = false

    override val withSearch: Boolean = false

    private fun createEnrichedAssets(): List<EnrichedAsset> {
        return (setOfNotNull(
            EnrichedAsset.createOrNull(session.bitcoin?.policyAsset),
            EnrichedAsset.createOrNull(session.liquid?.policyAsset)
        ) + (session.enrichedAssets.value.values.takeIf { session.liquid != null } ?: emptyList())).sortedWith(session::sortAssets)
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

        session.enrichedAssets.onEach {
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