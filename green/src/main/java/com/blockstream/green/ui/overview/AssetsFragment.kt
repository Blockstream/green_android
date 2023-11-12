package com.blockstream.green.ui.overview


import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.EnrichedAssetPair
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.overview.AssetsViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.AssetDetailsBottomSheetFragment
import com.blockstream.green.ui.items.AssetListItem
import com.blockstream.green.utils.observeFlow
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AssetsFragment :
    AppFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: AssetsFragmentArgs by navArgs()

    override val subtitle: String?
        get() = viewModel.greenWalletOrNull?.name

    val viewModel: AssetsViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        // Assets Balance
        val assetsAdapter = ModelAdapter<EnrichedAssetPair, AssetListItem> {
            AssetListItem(
                session = viewModel.session,
                assetPair = it,
                showBalance = true,
                isLoading = (it.first.assetId.isEmpty() && it.second == -1L)
            )
        }.observeFlow(
            lifecycleScope,
            viewModel.session.walletAssets,
            toList = {
                it.assets.map {
                    EnrichedAssetPair(EnrichedAsset.create(session = viewModel.session, assetId = it.key), it.value)
                }
            })

        val fastAdapter = FastAdapter.with(listOf(assetsAdapter))

        fastAdapter.onClickListener = { _, _, item, _ ->

            AssetDetailsBottomSheetFragment.show(
                assetId = item.assetPair.first.assetId,
                account = null,
                childFragmentManager
            )

            true
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }
}
