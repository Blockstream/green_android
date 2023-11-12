package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.data.EnrichedAsset
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

    override val withSearch: Boolean = true

    private fun createEnrichedAssets(): List<EnrichedAsset> {
        return listOfNotNull(
            EnrichedAsset.createOrNull(session = session, session.bitcoin?.policyAsset),
            EnrichedAsset.createOrNull(session = session, session.liquid?.policyAsset),
        ) + (session.enrichedAssets.value.takeIf { session.liquid != null }?.map {
            EnrichedAsset.create(session = session, assetId = it.assetId)
        } ?: listOf()) + listOfNotNull(
            EnrichedAsset.createAnyAsset(session = session, isAmp = false),
            EnrichedAsset.createAnyAsset(session = session, isAmp = true)
        ).sortedWith(session::sortEnrichedAssets)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showLoader = true
    }

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {

        val assetsAdapter = ModelAdapter<EnrichedAsset, AssetListItem> {
            AssetListItem(
                assetPair = it to 0L,
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
                item.assetPair.first.name(session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.assetPair.first.ticker(session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true || item.assetPair.first.isAnyAsset
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
    fun assetClicked(asset: EnrichedAsset)
}