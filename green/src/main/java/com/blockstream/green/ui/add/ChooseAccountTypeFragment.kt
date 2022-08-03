package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.NetworkLayer
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseAccountTypeFragmentBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.toggle
import com.blockstream.green.gdk.titleRes
import com.blockstream.green.ui.bottomsheets.ComingSoonBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.EnrichedAssetsBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.EnrichedAssetsListener
import com.blockstream.green.ui.items.AccountTypeListItem
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChooseAccountTypeFragment : AbstractAddAccountFragment<ChooseAccountTypeFragmentBinding>(
    R.layout.choose_account_type_fragment, 0
), EnrichedAssetsListener {
    val args: ChooseAccountTypeFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName by lazy { if (args.accountType == AccountType.UNKNOWN) "AddAccountChooseType" else "AddAccountChooseRecovery" }

    override val addAccountViewModel: AbstractAddAccountViewModel
        get() = viewModel

    override val assetId: String?
        get() = args.assetId

    @Inject
    lateinit var viewModelFactory: ChooseAccountTypeViewModel.AssistedFactory
    val viewModel: ChooseAccountTypeViewModel by viewModels {
        ChooseAccountTypeViewModel.provideFactory(viewModelFactory, args.wallet, args.assetId)
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedGuarded(view, savedInstanceState)
        binding.vm = viewModel

        viewModel.assetIdLiveData.observe(viewLifecycleOwner) {
            binding.asset.bind(scope = lifecycleScope, assetId = it, session = session, showEditIcon = true)
        }

        binding.assetMaterialCardView.setOnClickListener {
            EnrichedAssetsBottomSheetDialogFragment.show(fragmentManager = childFragmentManager)
        }

        binding.buttonAdvanced.setOnClickListener {
            viewModel.showAdvancedLiveData.toggle()
        }

        val fastAdapter = FastItemAdapter<GenericItem>()

        fastAdapter.onClickListener = { _, _, item: GenericItem, _: Int ->
            if (item is AccountTypeListItem) {

                val network = networkForAccountType(item.accountType)

                val nav = {
                    if (item.accountType == AccountType.TWO_OF_THREE) {
                        if (isBitcoin()) {
                            navigate(
                                ChooseAccountTypeFragmentDirections.actionChooseAccountTypeFragmentToAccount2of3Fragment(
                                    wallet = args.wallet,
                                    assetId = viewModel.assetId,
                                    layer = NetworkLayer.Bitcoin,
                                )
                            )
                        }else{
                            ComingSoonBottomSheetDialogFragment.show(childFragmentManager)
                        }
                    } else {
                        viewModel.createAccount(accountType = item.accountType, accountName = getString(item.accountType.titleRes()), network = network, null, null)
                    }
                }

                // Check if account is already archived
                if (viewModel.isAccountAlreadyArchived(network, item.accountType)) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.id_archived_account)
                        .setMessage(R.string.id_there_is_already_an_archived)
                        .setPositiveButton(R.string.id_continue) { _, _ ->
                            nav()
                        }
                        .setNeutralButton(R.string.id_archived_accounts) { _, _ ->
                            navigate(NavGraphDirections.actionGlobalArchivedAccountsFragment(wallet = wallet))
                        }
                        .show()
                } else {
                    nav()
                }
            }
            false
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        viewModel.accountTypesLiveData.observe(viewLifecycleOwner) {
            FastAdapterDiffUtil.set(adapter = fastAdapter.itemAdapter, items = it, detectMoves = true)
        }

        viewModel.onProgress.observe(viewLifecycleOwner) {
            binding.mainContainer.alpha = if(it) 0.2f else 1.0f
        }
    }

    private fun isBitcoin(): Boolean {
        return viewModel.assetId == BTC_POLICY_ASSET
    }

    private fun networkForAccountType(accountType: AccountType): Network {
        return when (accountType) {
            AccountType.BIP44_LEGACY,
            AccountType.BIP49_SEGWIT_WRAPPED,
            AccountType.BIP84_SEGWIT,
            AccountType.BIP86_TAPROOT -> {
                if (isBitcoin()) {
                    session.bitcoinSinglesig!!
                } else {
                    session.liquidSinglesig!!
                }
            }
            AccountType.STANDARD -> if (isBitcoin()) {
                session.bitcoinMultisig!!
            } else {
                session.liquidMultisig!!
            }
            AccountType.AMP_ACCOUNT -> session.liquidMultisig!!
            AccountType.TWO_OF_THREE -> session.bitcoinMultisig!!
            AccountType.UNKNOWN, AccountType.LIGHTNING -> throw Exception("Network not found")
        }
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    override fun assetClicked(assetId: String) {
        viewModel.assetId = assetId
    }
}