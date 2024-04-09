package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.events.Events
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.receive.RequestAmountViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.databinding.RequestAmountLabelBottomSheetBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.AmountTextWatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


/*
 * Request Label is hidden at the moment, as we need implementation from GDK
 * to save the address label
 */
class RequestAmountLabelBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RequestAmountLabelBottomSheetBinding, ReceiveViewModel>() {
    override val screenName = null
    override val segmentation = null

    override fun inflate(layoutInflater: LayoutInflater) = RequestAmountLabelBottomSheetBinding.inflate(layoutInflater)

    private val requestViewModel: RequestAmountViewModel by viewModel {
        parametersOf(
            viewModel.greenWallet,
            viewModel.accountAsset.value!!,
            viewModel.requestAmount.value ?: ""
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestViewModel.sideEffectAppFragment.onEach { sideEffect ->
            if(sideEffect is SideEffects.Dismiss){
                dismiss()
            }else if(sideEffect is SideEffects.Success){
                (parentFragment as AppFragment<*>).getGreenViewModel()?.postEvent(ReceiveViewModel.LocalEvents.SetRequestAmount(sideEffect.data as String?))
            }
        }.launchIn(lifecycleScope)

        binding.vm = requestViewModel
        binding.amountTextInputLayout.endIconCustomMode()

        AmountTextWatcher.watch(binding.amountEditText)

        binding.buttonOK.setOnClickListener {
            requestViewModel.postEvent(Events.Continue)
        }

        binding.buttonCurrency.setOnClickListener {
            requestViewModel.postEvent(RequestAmountViewModel.LocalEvents.ToggleCurrency)
        }

        binding.buttonClear.setOnClickListener {
            requestViewModel.amount.value = ""
            (parentFragment as AppFragment<*>).getGreenViewModel()?.postEvent(ReceiveViewModel.LocalEvents.SetRequestAmount(null))
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager) {
            show(RequestAmountLabelBottomSheetDialogFragment(), fragmentManager)
        }
    }
}