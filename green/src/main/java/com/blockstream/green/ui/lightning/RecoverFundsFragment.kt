package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.databinding.RecoverFundsFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.AbstractAccountWalletViewModel
import com.blockstream.green.ui.wallet.AbstractAssetWalletFragment
import com.blockstream.green.utils.getClipboard
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.text.NumberFormat

class RecoverFundsFragment :
    AbstractAssetWalletFragment<RecoverFundsFragmentBinding>(
        R.layout.recover_funds_fragment,
        menuRes = 0
    ) {
    override val screenName: String
        get() = if(isRefund) "OnChainRefund" else "LightningSweep"

    val args: RecoverFundsFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    private val isRefund by lazy {
        args.address != null
    }

    private val isSweep by lazy {
        args.address == null
    }

    override val title: String
        get() = getString(if (isRefund) R.string.id_refund else R.string.id_sweep)

    override val subtitle: String
        get() = wallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    override val showBalance: Boolean
        get() = false

    override val isRefundSwap: Boolean
        get() = true

    val viewModel: RecoverFundsViewModel by viewModel {
        parametersOf(
            args.wallet,
            AccountAsset.fromAccount(session.accounts.value.firstOrNull { it.isBitcoin && !it.isLightning }
                ?: session.activeAccount.value!!),
            args.address,
            args.amount
        )
    }

    override fun getAccountWalletViewModel(): AbstractAccountWalletViewModel {
        return viewModel
    }

    override val accountAssetLayoutBinding: AccountAssetLayoutBinding
        get() = binding.accountAsset

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Success){
            if(isRefund){
                dialog(R.string.id_refund, R.string.id_refund_initiated) {
                    popBackStack()
                }
            }else{
                dialog(R.string.id_sweep, R.string.id_sweep_initiated) {
                    popBackStack()
                }
            }
        }
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedGuarded(view, savedInstanceState)

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            viewLifecycleOwner
        ) { result ->
            if (result != null) {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.address.value = result
            }
        }

        binding.vm = viewModel

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { throwable ->
                errorDialog(throwable = throwable, errorReport = ErrorReport.create(throwable = throwable, network = session.lightning, session = session))
            }
        }

        binding.feeSlider.setLabelFormatter { value: Float ->
            val format = NumberFormat.getCurrencyInstance()
            format.maximumFractionDigits = 0
            format.format(value.toDouble())

            getString(
                when (value.toInt()) {
                    1 -> R.string.id_slow
                    2 -> R.string.id_medium
                    3 -> R.string.id_fast
                    else -> R.string.id_minimum
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
            viewModel.recoverFunds()
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

        binding.initialAddress.setOnClickListener {
            copyToClipboard(
                label = "Address",
                content = args.address ?: "",
                animateView = binding.initialAddressTextView,
                showCopyNotification = true
            )
        }
    }
}
