package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.ui.bottomsheets.AccountAssetBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.AccountAssetListener
import com.blockstream.green.ui.bottomsheets.ChooseAssetAccountBottomSheetDialogFragment


abstract class AbstractAssetWalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AbstractAccountWalletFragment<T>(layout, menuRes), AccountAssetListener {

    abstract val accountAssetLayoutBinding: AccountAssetLayoutBinding?
    open val showBalance: Boolean = true
    open val showEditIcon: Boolean = true
    open val showChooseAssetAccount: Boolean = false

    private val assetWalletViewModel
        get() = getAccountWalletViewModel() as AbstractAssetWalletViewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        assetWalletViewModel.accountAssetLiveData.observe(viewLifecycleOwner){
            accountAssetLayoutBinding?.bind(
                scope = lifecycleScope,
                accountAsset = it,
                session = session,
                showBalance = showBalance,
                showEditIcon = showEditIcon
            )
        }

        accountAssetLayoutBinding?.root?.setOnClickListener {
            if(showChooseAssetAccount){
                ChooseAssetAccountBottomSheetDialogFragment.show(fragmentManager = childFragmentManager)
                countly.assetChange(session)
            }else{
                AccountAssetBottomSheetDialogFragment.show(showBalance = showBalance, fragmentManager = childFragmentManager)
            }
        }
    }

    override fun accountAssetClicked(accountAsset: AccountAsset) {
        assetWalletViewModel.accountAsset = accountAsset
    }
}