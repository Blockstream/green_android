package com.blockstream.green.ui.wallet


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.AccountListItem
import com.blockstream.green.ui.overview.OverviewFragment
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.setNavigationResult
import com.blockstream.green.utils.show
import com.blockstream.green.utils.showPopupMenu
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ArchivedAccountsFragment :
    WalletFragment<BaseRecyclerViewBinding>(R.layout.base_recycler_view, 0) {
    val args: ArchivedAccountsFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    @Inject
    lateinit var viewModelFactory: ArchivedAccountsViewModel.AssistedFactory
    val viewModel: ArchivedAccountsViewModel by viewModels {
        ArchivedAccountsViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {

        binding.vm = viewModel

        val accountsModelAdapter = ModelAdapter { subAccount: SubAccount ->
            AccountListItem(
                session = session,
                subAccount = subAccount,
                isTopAccount = true,
                isAccountListOpen = true
            )
        }.observeList(viewLifecycleOwner, viewModel.getArchivedSubAccountsLiveData()) {
            if(it.isEmpty()){
               popBackStack()
            }
        }


        val fastAdapter = FastAdapter.with(accountsModelAdapter)

        fastAdapter.addClickListener<ListItemAccountBinding, AccountListItem>({ binding -> binding.buttonAccountMenu }) { v, _, _, item ->
            showPopupMenu(v, item.subAccount)
        }

        fastAdapter.onClickListener = { v, _, item, _ ->
            v?.let {
                showPopupMenu(it, item.subAccount)
            }
            true
        }

        binding.recycler.apply {
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }
    }

    private fun showPopupMenu(view: View, subaccount: SubAccount){
        showPopupMenu(view.findViewById(R.id.buttonAccountMenu),  R.menu.menu_account_unarchive ) { menuItem ->
            when (menuItem.itemId) {
                R.id.rename -> {
                    RenameAccountBottomSheetDialogFragment.newInstance(subaccount).show(this)
                }
//                R.id.archive -> {
//                    viewModel.updateSubAccountVisibility(subaccount, isHidden = true)
//                }
                R.id.unarchive -> {
                    viewModel.updateSubAccountVisibility(subaccount, isHidden = false)
                    setNavigationResult(result = subaccount.pointer, key = OverviewFragment.SET_ACCOUNT, destinationId = R.id.overviewFragment)
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
