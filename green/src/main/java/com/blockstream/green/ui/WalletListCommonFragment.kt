package com.blockstream.green.ui

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.DeviceBrand
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.databinding.WalletListCommonBinding
import com.blockstream.green.ui.bottomsheets.DeleteWalletBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.ui.items.DeviceBrandListItem
import com.blockstream.green.ui.items.WalletListItem
import com.blockstream.green.utils.showPopupMenu
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import javax.inject.Inject


abstract class WalletListCommonFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AppFragment<T>(layout, menuRes) {

    open val isDrawer = false

    @Inject
    lateinit var walletRepository: WalletRepository

    fun init(
        binding: WalletListCommonBinding,
        viewModel: WalletListCommonViewModel
    ) {
        binding.vm = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val softwareWalletsModel = ModelAdapter { model: Wallet ->
            WalletListItem(model, sessionManager.getWalletSession(model))
        }

        viewModel.wallets.observe(viewLifecycleOwner){
            softwareWalletsModel.set(it)
        }

        val softwareWalletsAdapter = FastAdapter.with(softwareWalletsModel)

        softwareWalletsAdapter.onClickListener = { _, _, item, _ ->
            navigate(item.wallet)
            closeDrawer()
            true
        }

        if(!isDrawer) {
            softwareWalletsAdapter.onLongClickListener = { view, _, item, _ ->
                showPopupMenu(view, R.menu.menu_wallet) { menuItem ->
                    when (menuItem.itemId) {
                        R.id.delete -> {
                            DeleteWalletBottomSheetDialogFragment.show(
                                item.wallet,
                                childFragmentManager
                            )
                        }

                        R.id.rename -> {
                            RenameWalletBottomSheetDialogFragment.show(
                                item.wallet,
                                childFragmentManager
                            )
                        }
                    }
                    true
                }

                true
            }
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
            navigate(NavGraphDirections.actionGlobalOverviewFragment(wallet))
        }else{
            navigate(NavGraphDirections.actionGlobalLoginFragment(wallet = wallet, autoLogin = true))
        }
    }
}