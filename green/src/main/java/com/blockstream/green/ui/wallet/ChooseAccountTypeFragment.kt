package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseAccountTypeFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.items.AccountTypeListItem
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChooseAccountTypeFragment : WalletFragment<ChooseAccountTypeFragmentBinding>(
    R.layout.choose_account_type_fragment, 0
) {
    val args: ChooseAccountTypeFragmentArgs by navArgs()
    override val wallet by lazy { args.wallet }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fastItemAdapter = createAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            if(item is AccountTypeListItem){
                if(item.isEnabled){
                    navigate(ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToAddAccountFragment(item.accountType, args.wallet))
                }
            }
            false
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastItemAdapter
        }
    }

    private fun createAdapter(): FastItemAdapter<GenericItem> {
        val adapter = FastItemAdapter<GenericItem>()

        if(wallet.isElectrum){
            adapter.add(AccountTypeListItem(AccountType.BIP49_SEGWIT_WRAPPED))
            adapter.add(AccountTypeListItem(AccountType.BIP84_SEGWIT))
        }else{
            adapter.add(AccountTypeListItem(AccountType.STANDARD))

            if(wallet.isLiquid){
                adapter.add(AccountTypeListItem(AccountType.AMP_ACCOUNT))
            }

            adapter.add(AccountTypeListItem(AccountType.TWO_OF_THREE).also {
                it.isEnabled = false
            })
        }

        return adapter
    }

    override fun getWalletViewModel(): WalletViewModel? = null
}