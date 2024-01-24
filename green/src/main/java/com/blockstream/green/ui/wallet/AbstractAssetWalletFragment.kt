package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.ui.bottomsheets.AccountAssetBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.AccountAssetListener
import com.blockstream.green.ui.bottomsheets.ChooseAssetAccountBottomSheetDialogFragment
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


abstract class AbstractAssetWalletFragment<T : ViewDataBinding> constructor(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int
) : AbstractAccountWalletFragment<T>(layout, menuRes), AccountAssetListener {

    abstract val accountAssetLayoutBinding: AccountAssetLayoutBinding?
    open val showBalance: Boolean = true
    open val showEditIcon: Boolean = true
    open val showChooseAssetAccount: Boolean = false
    open val isRefundSwap: Boolean = false

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        getGreenViewModel()?.accountAsset?.filterNotNull()?.onEach {
            accountAssetLayoutBinding?.bind(
                scope = lifecycleScope,
                accountAsset = it,
                session = session,
                showBalance = showBalance,
                showEditIcon = showEditIcon
            )
        }?.launchIn(lifecycleScope)

        if(showEditIcon) {
            accountAssetLayoutBinding?.root?.setOnClickListener {
                if (showChooseAssetAccount) {
                    ChooseAssetAccountBottomSheetDialogFragment.show(fragmentManager = childFragmentManager)
                    countly.assetChange(session)
                } else {
                    AccountAssetBottomSheetDialogFragment.show(
                        showBalance = showBalance,
                        isRefundSwap = isRefundSwap,
                        fragmentManager = childFragmentManager
                    )
                }
            }
        }
    }

    override fun accountAssetClicked(accountAsset: AccountAsset) {
        getGreenViewModel()?.accountAsset?.value = accountAsset
    }
}