package com.blockstream.green.ui.archived


import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.RenameAccountBottomSheetDialogFragment
import com.blockstream.green.ui.items.AccountListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ArchivedAccountsFragment :
    AppFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: ArchivedAccountsFragmentArgs by navArgs()

    val viewModel: ArchivedAccountsViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.AccountUnarchived) {
            if (args.navigateToOverview) {
                popBackStack(R.id.walletOverviewFragment, false)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        val titleAdapter = FastItemAdapter<GenericItem>()

        val accountsModelAdapter = ModelAdapter { account: Account ->
            AccountListItem(
                session = viewModel.session,
                account = account,
            )
        }.observeList(lifecycleScope, viewModel.archivedAccounts) {
            if (it.isEmpty()) {
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

        if (!viewModel.session.isWatchOnly) {
            fastAdapter.onClickListener = { v, _, item, _ ->
                if (item is AccountListItem) {
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
                R.id.unarchive -> {
                    viewModel.postEvent(Events.UnArchiveAccount(account = account))
                }
            }
            true
        }
    }
}
