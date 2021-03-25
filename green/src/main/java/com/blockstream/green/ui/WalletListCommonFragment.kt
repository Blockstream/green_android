package com.blockstream.green.ui

import android.content.Intent
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import com.blockstream.green.*
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.WalletListCommonBinding
import com.blockstream.green.ui.items.DeviceBrandListItem
import com.blockstream.green.ui.items.WalletListItem
import com.blockstream.green.utils.observe
import com.greenaddress.Bridge
import com.greenaddress.greenbits.ui.hardwarewallets.DeviceSelectorActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter

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

        val list: List<DeviceBrandListItem>

        if(Bridge.useGreenModule){
            list = listOf(DeviceBrandListItem(true))
        }else{
            list = listOf(DeviceBrandListItem(true),
                DeviceBrandListItem(false)
            )
        }

        val devicesAdapter = FastAdapter.with(ItemAdapter<DeviceBrandListItem>().add(list))

        devicesAdapter.onClickListener = { _, _, item, _ ->
            if(Bridge.useGreenModule) {
                navigate(NavGraphDirections.actionGlobalDeviceListFragment())
            }else{
                Bridge.bridgeSession(sessionManager.getHardwareSessionV3().gaSession, "mainnet",null)
                if(item.isBluetooth){
                    startActivity(Intent(requireContext(), DeviceSelectorActivity::class.java))
                }else{
                    navigate(NavGraphDirections.actionGlobalDeviceListFragment())
                }
            }
            closeDrawer()

            true
        }

        binding.recyclerSoftwareWallets.apply {
            adapter = softwareWalletsAdapter
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
            if(sessionManager.getWalletSession(wallet).isConnected()){
                // TODO open v4 Overview
            }else{
                navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
            }
        }else{
            navigate(NavGraphDirections.actionGlobalLoginFragment(wallet))
        }
    }
}