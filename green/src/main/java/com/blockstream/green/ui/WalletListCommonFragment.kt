package com.blockstream.green.ui

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.WalletListCommonBinding
import com.blockstream.green.devices.DeviceBrand
import com.blockstream.green.ui.items.DeviceBrandListItem
import com.blockstream.green.ui.items.WalletListItem
import com.blockstream.green.utils.observe
import com.greenaddress.Bridge
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

    internal val viewModel: MainViewModel by activityViewModels()

    fun init(binding: WalletListCommonBinding){
        binding.vm = viewModel

        val softwareWalletsAdapter = FastAdapter.with(ModelAdapter { model: Wallet ->
            WalletListItem(model)
        }.observe(viewLifecycleOwner, viewModel.wallets))

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
            if(Bridge.useGreenModule) {
                navigate(NavGraphDirections.actionGlobalDeviceListFragment(item.deviceBrand))
            }else{
                navigate(NavGraphDirections.actionGlobalDeviceListFragment(item.deviceBrand))
            }
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
    }

    private fun navigate(wallet: Wallet) {

        if(Bridge.useGreenModule){
            if(sessionManager.getWalletSession(wallet).isConnected){
                // TODO open v4 Overview
            }else{
                navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
            }
        }else{
            navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
        }
    }
}