package com.blockstream.green.ui

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.WalletListCommonBinding
import com.blockstream.DeviceBrand
import com.blockstream.green.ui.items.DeviceBrandListItem
import com.blockstream.green.ui.items.WalletListItem
import com.blockstream.green.ui.wallet.LoginFragmentDirections
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import javax.inject.Inject


abstract class WalletListCommonFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    @Inject
    lateinit var walletRepository: WalletRepository

    internal val activityViewModel: MainActivityViewModel by activityViewModels()

    fun init(binding: WalletListCommonBinding){
        binding.vm = activityViewModel

        val softwareWalletsAdapter = FastAdapter.with(ModelAdapter { model: Wallet ->
            WalletListItem(model, sessionManager.getWalletSession(model))
        }.observeList(viewLifecycleOwner, activityViewModel.wallets))

        softwareWalletsAdapter.onClickListener = { _, _, item, _ ->
            navigate(item.wallet)
            closeDrawer()
            true
        }

        val list: List<DeviceBrandListItem> = listOf(
            DeviceBrandListItem(DeviceBrand.Blockstream),
            DeviceBrandListItem(DeviceBrand.Ledger),
            DeviceBrandListItem(DeviceBrand.Trezor)
        )

        val devicesAdapter = FastAdapter.with(ItemAdapter<DeviceBrandListItem>().add(list))

        devicesAdapter.onClickListener = { _, _, item, _ ->
            navigate(NavGraphDirections.actionGlobalDeviceListFragment(item.deviceBrand))
            closeDrawer()

            true
        }

        binding.recyclerSoftwareWallets.apply {
            adapter = softwareWalletsAdapter
            itemAnimator = SlideDownAlphaAnimator()
        }

        binding.recyclerDevices.apply {
            adapter = devicesAdapter
        }

        binding.addWallet.setOnClickListener {
            navigate(IntroFragmentDirections.actionGlobalAddWalletFragment())
            closeDrawer()
        }

        sessionManager.connectionChangeEvent.observe(viewLifecycleOwner){
            softwareWalletsAdapter.notifyAdapterDataSetChanged()
        }
    }

    internal fun navigate(wallet: Wallet) {
        val walletSession = sessionManager.getWalletSession(wallet)

        if(walletSession.isConnected){
            navigate(LoginFragmentDirections.actionGlobalOverviewFragment(wallet))
        }else{
            navigate(NavGraphDirections.actionGlobalLoginFragment(wallet = wallet, autoLogin = true))
        }
    }
}