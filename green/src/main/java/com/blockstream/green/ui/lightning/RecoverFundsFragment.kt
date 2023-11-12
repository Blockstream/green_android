package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.lightning.RecoverFundsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.RecoverFundsFragmentBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.filters.NumberValueFilter
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.AccountAssetBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.utils.getClipboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.text.NumberFormat

class RecoverFundsFragment : AppFragment<RecoverFundsFragmentBinding>(
    R.layout.recover_funds_fragment,
    menuRes = 0
) {
    val args: RecoverFundsFragmentArgs by navArgs()

    override val title: String
        get() = getString(if (viewModel.isRefund) R.string.id_refund else if(viewModel.isSendAll) R.string.id_empty_lightning_account else R.string.id_sweep)

    override val subtitle: String
        get() = viewModel.greenWallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    val viewModel: RecoverFundsViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.isSendAll,
            args.address,
            args.amount
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Success){
            if(viewModel.isRefund){
                dialog(R.string.id_refund, R.string.id_refund_initiated) {
                    findNavController().popBackStack(R.id.walletOverviewFragment, false)
                }
            }else{
                dialog(R.string.id_sweep, R.string.id_sweep_initiated) {
                    findNavController().popBackStack(R.id.walletOverviewFragment, false)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.accountAsset.filterNotNull().onEach {
            binding.accountAsset.bind(
                scope = lifecycleScope,
                accountAsset = it,
                session = viewModel.session,
                showBalance = false,
                showEditIcon = true
            )
        }.launchIn(lifecycleScope)

        binding.accountAsset.root.setOnClickListener {
            AccountAssetBottomSheetDialogFragment.show(
                showBalance = false,
                isRefundSwap = true,
                fragmentManager = childFragmentManager
            )
        }

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) { result ->
            if (result != null) {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.address.value = result.result
            }
        }

        binding.vm = viewModel

        binding.feeSlider.setLabelFormatter { value: Float ->
            val format = NumberFormat.getCurrencyInstance()
            format.maximumFractionDigits = 0
            format.format(value.toDouble())

            getString(
                when (value.toInt()) {
                    1 -> R.string.id_minimum
                    2 -> R.string.id_slow
                    3 -> R.string.id_medium
                    4 -> R.string.id_fast
                    else ->  R.string.id_custom
                }
            )
        }

        binding.buttonAddressClear.setOnClickListener {
            viewModel.address.value = ""
        }

        binding.buttonAddressPaste.setOnClickListener {
            viewModel.address.value = getClipboard(requireContext()) ?: ""
        }

        binding.buttonConfirm.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }

        binding.buttonAddressScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(
                screenName = screenName,
                fragmentManager = childFragmentManager
            )
        }

        binding.showAddressToggle.addOnButtonCheckedListener { _, _, isChecked ->
            viewModel.showManualAddress.value = isChecked
        }

        binding.buttonEditFee.setOnClickListener {
            setCustomFeeRate()
        }
    }

    private fun setCustomFeeRate(){
        val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))
        dialogBinding.textInputLayout.endIconCustomMode()

        viewModel.feeAmountRate
        // TODO add locale
        dialogBinding.textInputLayout.placeholderText = "0"
        dialogBinding.editText.keyListener = NumberValueFilter(0)
        dialogBinding.text = viewModel.customFee.value.toString()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.id_default_custom_fee_rate)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                try {
                    dialogBinding.text.let { input ->

                        if (input.isNullOrBlank()) {
                            viewModel.postEvent(RecoverFundsViewModel.LocalEvents.SetCustomFee(null))
                        } else {
                            viewModel.postEvent(RecoverFundsViewModel.LocalEvents.SetCustomFee((dialogBinding.text?.toLong() ?: 0L)))
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbar(R.string.id_error_setting_fee_rate, Snackbar.LENGTH_SHORT)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    }
}
