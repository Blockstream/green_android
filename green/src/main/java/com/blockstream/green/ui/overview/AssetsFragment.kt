package com.blockstream.green.ui.overview


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.gdk.AssetPair
import com.blockstream.green.ui.bottomsheets.AssetDetailsBottomSheetFragment
import com.blockstream.green.ui.items.AssetListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.observeMap
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@AndroidEntryPoint
class AssetsFragment :
    AbstractWalletFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: AssetsFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "Assets"

    override val subtitle: String?
        get() = if (isSessionNetworkInitialized) wallet.name else null

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {

        binding.vm = viewModel

        // Assets Balance
        val assetsAdapter = ModelAdapter<AssetPair, AssetListItem> {
            AssetListItem(
                session = session,
                assetPair = it,
                showBalance = true,
                isLoading = (it.first.isEmpty() && it.second == -1L)
            )
        }.observeMap(
            lifecycleScope,
            session.walletAssetsFlow as Flow<Map<*, *>>,
            toModel = {
                AssetPair(it.key as String, (it.value as Long))
            })

        val fastAdapter = FastAdapter.with(listOf(assetsAdapter))

        fastAdapter.onClickListener = { _, _, item, _ ->

            AssetDetailsBottomSheetFragment.show(
                assetId = item.assetPair.first,
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
