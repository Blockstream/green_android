package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.gdk.assetName
import com.blockstream.green.gdk.assetTicker
import com.blockstream.green.gdk.balance
import com.blockstream.green.ui.items.AccountAssetListItem
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class AccountAssetBottomSheetDialogFragment : FilterBottomSheetDialogFragment(),
    FilterableDataProvider {

    @Suppress("UNCHECKED_CAST")
    internal val viewModel: AbstractWalletViewModel by lazy {
        (requireParentFragment() as AbstractWalletFragment<*>).getWalletViewModel()
    }

    val session
        get() = viewModel.session

    val account: Account? by lazy { arguments?.getParcelable(ACCOUNT) }
    val showBalance: Boolean by lazy { arguments?.getBoolean(SHOW_BALANCE, true) ?: true}
    override val withDivider: Boolean = false

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {
        val accountAssetsAdapter = ModelAdapter<AccountAsset, AccountAssetListItem> {
            AccountAssetListItem(
                scope = lifecycleScope,
                accountAsset = it,
                session = session,
                showBalance = showBalance
            )
        }.observeList(lifecycleScope, session.accountAssetFlow.map { list ->
            list.filter { it.balance(session) > 0 } // Filter only AccountAsset with funds
        }.map { list ->
            when {
                account != null -> list.filter { it.account.id == account!!.id }
                else -> list
            }
        })

        accountAssetsAdapter.itemFilter.filterPredicate =
            { item: AccountAssetListItem, constraint: CharSequence? ->
                item.accountAsset.account.name.lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.accountAsset.assetName(session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.accountAsset.assetTicker(session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true
            }

        return accountAssetsAdapter
    }

    override fun getFilterHeaderAdapter(requestCode: Int): GenericFastItemAdapter? {
        return null
    }

    override fun getFilterFooterAdapter(requestCode: Int): GenericFastItemAdapter? {
        return null
    }

    override fun filteredItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        requireParentFragment().also {
            if (it is AccountAssetListener) {
                it.accountAssetClicked((item as AccountAssetListItem).accountAsset)
            }
        }
    }

    companion object {
        private const val ACCOUNT = "ACCOUNT"
        private const val SHOW_BALANCE = "SHOW_BALANCE"

        fun show(
            fragmentManager: FragmentManager,
            account: Account? = null,
            showBalance: Boolean = true,
        ) {
            showSingle(AccountAssetBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(ACCOUNT, account)
                    bundle.putBoolean(SHOW_BALANCE, showBalance)
                    bundle.putBoolean(WITH_SEARCH, false)
                }
            }, fragmentManager)
        }
    }
}

interface AccountAssetListener {
    fun accountAssetClicked(accountAsset: AccountAsset)
}