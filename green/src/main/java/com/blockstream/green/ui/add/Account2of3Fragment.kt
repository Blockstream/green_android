package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.add.Account2of3ViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.Account2of3FragmentBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.bottomsheets.ComingSoonBottomSheetDialogFragment
import com.blockstream.green.ui.items.ContentCardListItem
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class Account2of3Fragment : AbstractAddAccountFragment<Account2of3FragmentBinding>(
    R.layout.account_2of3_fragment, 0
) {
    val args: Account2of3FragmentArgs by navArgs()

    override val assetId: String?
        get() = args.setupArgs.assetId

    override val title: String?
        get() = args.setupArgs.network?.canonicalName

    override val toolbarIcon: Int?
        get() = args.setupArgs.network?.getNetworkIcon()

    override val network: Network?
        get() = args.setupArgs.network

    override val viewModel: Account2of3ViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    enum class TwoOfThreeRecovery {
        HARDWARE_WALLET, NEW_RECOVERY, EXISTING_RECOVERY, XPUB
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.NavigateTo) {
            (sideEffect.destination as? NavigateDestinations.NewRecovery)?.also {
                navigate(
                    Account2of3FragmentDirections.actionAccount2of3FragmentToRecoveryIntroFragment(
                        setupArgs = it.setupArgs
                    )
                )
            }
            (sideEffect.destination as? NavigateDestinations.ExistingRecovery)?.also {
                navigate(
                    Account2of3FragmentDirections.actionAccount2of3FragmentToEnterRecoveryPhraseFragment(
                        setupArgs = it.setupArgs
                    )
                )
            }
            (sideEffect.destination as? NavigateDestinations.Xpub)?.also {
                navigate(
                    Account2of3FragmentDirections.actionAccount2of3FragmentToXpubFragment(
                        setupArgs = it.setupArgs
                    )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fastItemAdapter = createAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _: Int ->
            if (item is ContentCardListItem) {
                when (item.key) {
                    TwoOfThreeRecovery.NEW_RECOVERY -> {
                        viewModel.postEvent(Account2of3ViewModel.LocalEvents.NewRecovery)
                    }
                    TwoOfThreeRecovery.EXISTING_RECOVERY -> {
                        viewModel.postEvent(Account2of3ViewModel.LocalEvents.ExistingRecovery)
                    }
                    TwoOfThreeRecovery.XPUB -> {
                        viewModel.postEvent(Account2of3ViewModel.LocalEvents.Xpub)
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

//        adapter.add(
//            ContentCardListItem(
//                key = TwoOfThreeRecovery.HARDWARE_WALLET,
//                title = StringHolder(R.string.id_hardware_wallet),
//                caption = StringHolder(R.string.id_use_a_hardware_wallet_as_your)
//            )
//        )

        adapter.add(
            ContentCardListItem(
                key = TwoOfThreeRecovery.NEW_RECOVERY,
                title = StringHolder(R.string.id_new_recovery_phrase),
                caption = StringHolder(R.string.id_generate_a_new_recovery_phrase)
            )
        )

        adapter.add(
            ContentCardListItem(
                key = TwoOfThreeRecovery.EXISTING_RECOVERY,
                title = StringHolder(R.string.id_existing_recovery_phrase),
                caption = StringHolder(R.string.id_use_an_existing_recovery_phrase)
            )
        )

        adapter.add(
            ContentCardListItem(
                key = TwoOfThreeRecovery.XPUB,
                title = StringHolder(R.string.id_use_a_public_key),
                caption = StringHolder(R.string.id_use_an_xpub_for_which_you_own)
            )
        )

        return adapter
    }
}