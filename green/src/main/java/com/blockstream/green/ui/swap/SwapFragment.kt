package com.blockstream.green.ui.swap

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.TransactionSegmentation
import com.blockstream.common.TransactionType
import com.blockstream.common.extensions.getAssetName
import com.blockstream.common.extensions.getAssetTicker
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.data.SwapProposal
import com.blockstream.common.gdk.data.Utxo
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.SwapFragmentBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.FilterBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.FilterableDataProvider
import com.blockstream.green.ui.items.AssetSmallListItem
import com.blockstream.green.ui.items.UtxoListItem
import com.blockstream.green.ui.twofactor.DialogTwoFactorResolver
import com.blockstream.green.utils.toAmountLook
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import kotlinx.coroutines.FlowPreview
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@FlowPreview
class SwapFragment : AppFragment<SwapFragmentBinding>(
    layout = R.layout.swap_fragment,
    menuRes = 0
), FilterableDataProvider {
    override val isAdjustResize = false

    val args: SwapFragmentArgs by navArgs()

    override val screenName = "Swap"

    val viewModel: SwapViewModel by viewModel {
        parametersOf(args.wallet, args.proposal)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if (sideEffect is SideEffects.Navigate) {
            (sideEffect.data as? SwapProposal)?.also {
                navigate(
                    SwapFragmentDirections.actionSwapFragmentToSwapProposalFragment(
                        wallet = viewModel.greenWallet,
                        proposal = it
                    )
                )
            }
            (sideEffect.data as? CreateTransaction)?.also {
                navigate(
                    SwapFragmentDirections.actionSwapFragmentToSendConfirmFragment(
                        wallet = viewModel.greenWallet,
                        accountAsset = viewModel.enabledAccounts.first().accountAsset
                    )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel

        viewModel.proposal?.let {
            binding.buttonSwap.visibility = View.GONE
            binding.buttonTx.visibility = View.VISIBLE
        }

        viewModel.inputLiveData.observe(viewLifecycleOwner) { inputOrNull ->
            inputOrNull?.let { assetSwap ->
                binding.from.bind(
                    scope = lifecycleScope,
                    assetId = assetSwap.assetId,
                    session = viewModel.session,
                    primaryValue = {
                        assetSwap.amount.toAmountLook(
                            viewModel.session,
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
                    session = viewModel.session,
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
                    session = viewModel.session,
                    primaryValue = {
                        utxo.satoshi.toAmountLook(
                            viewModel.session,
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
                binding.to.bind(scope = lifecycleScope, assetId = assetId, session = viewModel.session, showBalance = false)
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
            viewModel.createSwapProposal(twoFactorResolver = DialogTwoFactorResolver(this))
        }

        binding.buttonTx.setOnClickListener {
            viewModel.completeSwapProposal(twoFactorResolver = DialogTwoFactorResolver(this))
        }

        binding.switchExchangeRate.setOnClickListener {
            viewModel.switchExchangeRate()
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    override fun getFilterAdapter(requestCode: Int): ModelAdapter<*, *> {
        return if(requestCode == 1) {
            val adapter = ModelAdapter<Utxo, UtxoListItem> {
                UtxoListItem(scope = lifecycleScope, utxo = it, session = viewModel.session, showHash = false, showName = true)
            }.set(viewModel.utxos)

            adapter.itemFilter.filterPredicate = { item: UtxoListItem, constraint: CharSequence? ->
                item.utxo.assetId.getAssetName(viewModel.session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.utxo.assetId.getAssetTicker(viewModel.session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true
            }

            adapter
        }else{

            val adapter = ModelAdapter<String, AssetSmallListItem> {
                AssetSmallListItem(assetId = it, session = viewModel.session)
            }.set(viewModel.toAssets)

            adapter.itemFilter.filterPredicate = { item: AssetSmallListItem, constraint: CharSequence? ->
                item.assetId.getAssetName(viewModel.session).lowercase().contains(
                    constraint.toString().lowercase()
                ) || item.assetId.getAssetTicker(viewModel.session)?.lowercase()?.contains(
                    constraint.toString().lowercase()
                ) == true
            }

            adapter
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
