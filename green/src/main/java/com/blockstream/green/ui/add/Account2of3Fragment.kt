package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.databinding.Account2of3FragmentBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.gdk.network
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.bottomsheets.ComingSoonBottomSheetDialogFragment
import com.blockstream.green.ui.items.ContentCardListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Account2of3Fragment : AbstractWalletFragment<Account2of3FragmentBinding>(
    R.layout.account_2of3_fragment, 0
) {
    val args: Account2of3FragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName by lazy { "AddAccountChooseRecovery" }

    override val title: String?
        get() = args.layer.network(session)?.canonicalName

    override val toolbarIcon: Int?
        get() = args.layer.network(session)?.getNetworkIcon()

    @Inject
    lateinit var gdkBridge: GdkBridge

    @Inject
    lateinit var viewModelFactory: WalletViewModel.AssistedFactory
    val viewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    enum class TwoOfThreeRecovery {
        HARDWARE_WALLET, NEW_RECOVERY, EXISTING_RECOVERY, XPUB
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val fastItemAdapter = createAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _: Int ->
            if (item is ContentCardListItem) {
                val network = if (args.layer.isBitcoin) {
                    session.bitcoinSinglesig
                } else {
                    session.liquidSinglesig
                } ?: session.defaultNetwork

                when (item.key) {
                    TwoOfThreeRecovery.NEW_RECOVERY -> {
                        navigate(
                            Account2of3FragmentDirections.actionAccount2of3FragmentToRecoveryIntroFragment(
                                wallet = args.wallet,
                                assetId = args.assetId,
                                network = network
                            )
                        )
                    }
                    TwoOfThreeRecovery.EXISTING_RECOVERY -> {
                        navigate(
                            Account2of3FragmentDirections.actionAccount2of3FragmentToEnterRecoveryPhraseFragment(
                                wallet = args.wallet,
                                assetId = args.assetId,
                                network = network,
                                isAddAccount = true
                            )
                        )
                    }
                    TwoOfThreeRecovery.XPUB -> {
                        navigate(
                            Account2of3FragmentDirections.actionAccount2of3FragmentToEnterXpubFragment(
                                wallet = args.wallet,
                                assetId = args.assetId,
                                network = network,
                                accountType = AccountType.TWO_OF_THREE
                            )
                        )
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
    }

    private fun createAdapter(): FastItemAdapter<GenericItem> {
        val adapter = FastItemAdapter<GenericItem>()

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

        return adapter
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel
}