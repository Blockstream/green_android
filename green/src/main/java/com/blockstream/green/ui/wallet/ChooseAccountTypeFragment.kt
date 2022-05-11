package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseAccountTypeFragmentBinding
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.bottomsheets.ComingSoonBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.MenuDataProvider
import com.blockstream.green.ui.items.AccountTypeListItem
import com.blockstream.green.ui.items.ContentCardListItem
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChooseAccountTypeFragment : WalletFragment<ChooseAccountTypeFragmentBinding>(
    R.layout.choose_account_type_fragment, 0
) {
    val args: ChooseAccountTypeFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName by lazy { if (args.accountType == AccountType.UNKNOWN) "AddAccountChooseType" else "AddAccountChooseRecovery" }

    @Inject
    lateinit var greenWallet: GreenWallet

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, wallet)
    }

    enum class TwoOfThreeRecovery{
        HARDWARE_WALLET, NEW_RECOVERY, EXISTING_RECOVERY, XPUB
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val fastItemAdapter = createAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _: Int ->
            if(item is AccountTypeListItem){
                if(item.accountType == AccountType.TWO_OF_THREE){
                    if (wallet.isLiquid) {
                        ComingSoonBottomSheetDialogFragment.show(childFragmentManager)
                    }else{
                        navigate(
                            ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentSelf(
                                accountType = AccountType.TWO_OF_THREE,
                                wallet = args.wallet
                            )
                        )
                    }
                }else{
                    navigate(ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToAddAccountFragment(accountType = item.accountType, wallet = args.wallet))
                }
            }else if(item is ContentCardListItem){
                when(item.key){
                    TwoOfThreeRecovery.NEW_RECOVERY -> {

                        MenuBottomSheetDialogFragment.show(object : MenuDataProvider {
                            override fun getTitle() = getString(R.string.id_new_recovery_phrase)
                            override fun getSubtitle() = getString(R.string.id_choose_recovery_phrase_length)

                            override fun getMenuListItems() = listOf(
                                MenuListItem(title = StringHolder(R.string.id_12_words)),
                                MenuListItem(title = StringHolder(R.string.id_24_words))
                            )

                            override fun menuItemClicked(item: GenericItem, position: Int) {
                                val mnemonic = if (position == 0) greenWallet.generateMnemonic12() else greenWallet.generateMnemonic24()
                                navigate(ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToRecoveryIntroFragment(wallet = args.wallet, mnemonic = mnemonic))
                            }

                        }, childFragmentManager)

                    }
                    TwoOfThreeRecovery.EXISTING_RECOVERY -> {
                        navigate(ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToEnterRecoveryPhraseFragment(wallet = args.wallet, isAddAccount = true))
                    }
                    TwoOfThreeRecovery.XPUB -> {
                        navigate(ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToEnterXpubFragment(wallet = args.wallet, accountType = args.accountType))
                    }
                    else -> {
                        ComingSoonBottomSheetDialogFragment.show(childFragmentManager)
                    }
                }
            }
            false
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastItemAdapter
        }

        binding.subtitle.text = getString(if(args.accountType == AccountType.TWO_OF_THREE) R.string.id_select_your_recovery_key else R.string.id_what_type_of_account_would_you)

    }

    private fun createAdapter(): FastItemAdapter<GenericItem> {
        val adapter = FastItemAdapter<GenericItem>()

        if(wallet.isElectrum){
            adapter.add(AccountTypeListItem(AccountType.BIP49_SEGWIT_WRAPPED))
            adapter.add(AccountTypeListItem(AccountType.BIP84_SEGWIT))
        }else{
            if(args.accountType == AccountType.TWO_OF_THREE){

                adapter.add(
                    ContentCardListItem(
                        key = TwoOfThreeRecovery.HARDWARE_WALLET,
                        title = StringHolder(R.string.id_hardware_wallet),
                        caption = StringHolder(R.string.id_use_a_hardware_wallet_as_your)
                    )
                )
                adapter.add(
                    ContentCardListItem(
                        key = TwoOfThreeRecovery.NEW_RECOVERY,
                        title = StringHolder(R.string.id_new_recovery_phrase),
                        caption = StringHolder(R.string.id_generate_a_new_recovery_phrase)
                    )
                )

                val expandable = TitleExpandableListItem(StringHolder(R.string.id_more_options))
                expandable.subItems.add(
                    ContentCardListItem(
                        key = TwoOfThreeRecovery.EXISTING_RECOVERY,
                        title = StringHolder(R.string.id_existing_recovery_phrase),
                        caption = StringHolder(R.string.id_use_an_existing_recovery_phrase)
                    )
                )

                expandable.subItems.add(
                    ContentCardListItem(
                        key = TwoOfThreeRecovery.XPUB,
                        title = StringHolder(R.string.id_use_a_public_key),
                        caption = StringHolder(R.string.id_use_an_xpub_for_which_you_own)
                    )
                )

                adapter.getExpandableExtension()
                adapter.add(expandable)
            } else {
                adapter.add(AccountTypeListItem(AccountType.STANDARD))

                if (wallet.isLiquid) {
                    adapter.add(AccountTypeListItem(AccountType.AMP_ACCOUNT))
                }

                if (!wallet.isLiquid) {
                    adapter.add(AccountTypeListItem(AccountType.TWO_OF_THREE))
                }
            }
        }

        return adapter
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel
}