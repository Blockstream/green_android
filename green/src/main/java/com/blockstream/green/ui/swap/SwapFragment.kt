package com.blockstream.green.ui.swap

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.data.CreateTransaction
import com.blockstream.gdk.data.SwapProposal
import com.blockstream.gdk.data.Utxo
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.data.TransactionSegmentation
import com.blockstream.green.data.TransactionType
import com.blockstream.green.databinding.SwapFragmentBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.gdk.getAssetName
import com.blockstream.green.gdk.getAssetTicker
import com.blockstream.green.ui.bottomsheets.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.FilterableDataProvider
import com.blockstream.green.ui.items.AssetSmallListItem
import com.blockstream.green.ui.items.UtxoListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.toAmountLook
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SwapFragment : AbstractWalletFragment<SwapFragmentBinding>(
    layout = R.layout.swap_fragment,
    menuRes = 0
), FilterableDataProvider {
    override val isAdjustResize = false

    val args: SwapFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "Swap"

    @Inject
    lateinit var viewModelFactory: SwapViewModel.AssistedFactory
    val viewModel: SwapViewModel by viewModels {
        SwapViewModel.provideFactory(
            viewModelFactory,
            args.wallet,
            args.proposal
         )
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let {
                errorDialog(it)
            }
        }

        viewModel.proposal?.let {
            binding.buttonSwap.visibility = View.GONE
            binding.buttonTx.visibility = View.VISIBLE
        }

        viewModel.inputLiveData.observe(viewLifecycleOwner) { inputOrNull ->
            inputOrNull?.let { assetSwap ->
                binding.from.bind(
                    scope = lifecycleScope,
                    assetId = assetSwap.assetId,
                    session = session,
                    primaryValue = {
                        assetSwap.amount.toAmountLook(
                            session,
                            assetId = assetSwap.assetId,
                            withUnit = true,
                            withGrouping = true,
                            withMinimumDigits = false
                        )
                    },
                    showBalance = true
                )
            }
        }
        viewModel.outputLiveData.observe(viewLifecycleOwner) { outputOrNull ->
            outputOrNull?.let { assetSwap ->
                binding.to.bind(
                    scope = lifecycleScope,
                    assetId = assetSwap.assetId,
                    session = session,
                    showBalance = false
                )
                binding.amountTextInputLayout.isEnabled = false
                binding.amountInputEditText.isEnabled = false
            }
        }

        viewModel.utxoLiveData.observe(viewLifecycleOwner) { utxoOrNull ->
            utxoOrNull?.let { utxo ->
                binding.from.bind(
                    scope = lifecycleScope,
                    assetId = utxo.assetId,
                    session = session,
                    primaryValue = {
                        utxo.satoshi.toAmountLook(
                            session,
                            assetId = utxo.assetId,
                            withUnit = true,
                            withGrouping = true,
                            withMinimumDigits = false
                        )
                    },
                    showBalance = true
                )
            }
        }

        viewModel.toAssetIdLiveData.observe(viewLifecycleOwner){ assetIdOrNull ->
            assetIdOrNull?.let { assetId ->
                binding.to.bind(scope = lifecycleScope, assetId = assetId, session = session, showBalance = false)
            }
        }

        binding.amountTextInputLayout.endIconCustomMode()

        binding.fromCard.setOnClickListener {
            FilterBottomSheetDialogFragment.show(requestCode = 1, withDivider = false, fragmentManager = childFragmentManager)
        }

        binding.toCard.setOnClickListener {
            FilterBottomSheetDialogFragment.show(requestCode = 2, withDivider = false, fragmentManager = childFragmentManager)
        }

        binding.buttonSwap.setOnClickListener {
            viewModel.createSwapProposal(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
        }

        binding.buttonTx.setOnClickListener {
            viewModel.completeSwapProposal(twoFactorResolver = DialogTwoFactorResolver(requireContext()))
        }

        binding.switchExchangeRate.setOnClickListener {
            viewModel.switchExchangeRate()
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                errorDialog(throwable)
            }
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
            onEvent.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let {
                (it.data as? SwapProposal)?.let {
                    navigate(
                        SwapFragmentDirections.actionSwapFragmentToSwapProposalFragment(
                            wallet = wallet,
                            proposal = it
                        )
                    )
                }
                (it.data as? CreateTransaction)?.let {
                    navigate(
                        SwapFragmentDirections.actionSwapFragmentToSendConfirmFragment(
                            wallet = wallet,
                            account = viewModel.enabledAccounts.first(),
                            transactionSegmentation = TransactionSegmentation(
                                transactionType = TransactionType.SWAP,
                                addressInputType = null,
                                sendAll = false
                            )
                        )
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {
        return if(requestCode == 1) {
            val adapter = ModelAdapter<Utxo, UtxoListItem> {
                UtxoListItem(scope = lifecycleScope, utxo = it, session = session, showHash = false, showName = true)
            }.set(viewModel.utxos)

            adapter.itemFilter.filterPredicate = { item: UtxoListItem, constraint: CharSequence? ->
                item.utxo.assetId.getAssetName(session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.utxo.assetId.getAssetTicker(session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true
            }

            return adapter

        }else{

            val adapter = ModelAdapter<String, AssetSmallListItem> {
                AssetSmallListItem(assetId = it, session = session)
            }.set(viewModel.toAssets)

            adapter.itemFilter.filterPredicate = { item: AssetSmallListItem, constraint: CharSequence? ->
                item.assetId.getAssetName(session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.assetId.getAssetTicker(session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true
            }

            return adapter
        }
    }

    override fun getFilterHeaderAdapter(requestCode: Int): GenericFastItemAdapter? {
        return null
    }

    override fun getFilterFooterAdapter(requestCode: Int): GenericFastItemAdapter? {
        return null
    }

    override fun filteredItemClicked(requestCode: Int, item: GenericItem, position: Int) {
        if(requestCode == 1) {
            viewModel.utxo = (item as UtxoListItem).utxo
        }else{
            viewModel.toAssetId = (item as AssetSmallListItem).assetId
        }
    }
}
