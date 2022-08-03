package com.blockstream.green.ui.archived


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.belongsToLayer
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.bottomsheets.RenameAccountBottomSheetDialogFragment
import com.blockstream.green.ui.items.AccountListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.observeList
import com.blockstream.green.extensions.showPopupMenu
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class ArchivedAccountsFragment :
    AbstractWalletFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: ArchivedAccountsFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }


    override val screenName = "ArchivedAccounts"

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val layer = args.layer

        binding.vm = viewModel

        val titleAdapter = FastItemAdapter<GenericItem>()

        val accountsModelAdapter = ModelAdapter { account: Account ->
            AccountListItem(
                session = session,
                account = account,
            )
        }.observeList(lifecycleScope, session.allAccountsFlow.map { accounts ->
            accounts.filter { (layer == null || it.belongsToLayer(layer)) && it.hidden }
        }) {
            if (it.isEmpty()) {
                // findNavController().popBackStack(R.id.walletOverviewFragment, false)
                titleAdapter.set(
                    listOf(
                        TextListItem(
                            text = StringHolder(R.string.id_no_archived_accounts),
                            textColor = R.color.color_on_surface_emphasis_low,
                            textAlignment = View.TEXT_ALIGNMENT_CENTER,
                            paddingTop = R.dimen.dp32,
                            paddingBottom = R.dimen.dp24,
                            paddingLeft = R.dimen.dp24,
                            paddingRight = R.dimen.dp24
                        )
                    )
                )
            } else {
                titleAdapter.clear()
            }
        }

        val fastAdapter = FastAdapter.with(listOf(titleAdapter, accountsModelAdapter))

        if (!session.isWatchOnly) {
            fastAdapter.onClickListener = { v, _, item, _ ->
                if(item is AccountListItem) {
                    v?.let {
                        showPopupMenu(it, item.account)
                    }
                }
                true
            }
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }

    private fun showPopupMenu(view: View, account: Account) {
        showPopupMenu(
            view,
            R.menu.menu_account_unarchive
        ) { menuItem ->
            when (menuItem.itemId) {
                R.id.rename -> {
                    RenameAccountBottomSheetDialogFragment.show(account, childFragmentManager)
                }
//                R.id.archive -> {
//                    viewModel.updateSubAccountVisibility(subaccount, isHidden = true)
//                }
                R.id.unarchive -> {
                    viewModel.updateAccountVisibility(account = account, isHidden = false)
                }
//                R.id.view_account -> {
//                    setNavigationResult(result = subaccount.pointer, key = OverviewFragment.SET_ACCOUNT, destinationId = R.id.overviewFragment)
//                    findNavController().popBackStack(R.id.overviewFragment, false)
//                }
            }
            true
        }
    }
}
