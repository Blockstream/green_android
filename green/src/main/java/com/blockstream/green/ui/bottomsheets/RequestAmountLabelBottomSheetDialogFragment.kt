package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.blockstream.green.databinding.RequestAmountLabelBottomSheetBinding
import com.blockstream.green.ui.receive.ReceiveViewModel
import com.blockstream.green.ui.receive.RequestAmountLabelViewModel
import com.blockstream.green.utils.AmountTextWatcher
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.endIconCopyMode
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject


/*
 * Request Label is hidden at the moment, as we need implementation from GDK
 * to save the address label
 */
@AndroidEntryPoint
class RequestAmountLabelBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RequestAmountLabelBottomSheetBinding, ReceiveViewModel>() {
    val session by lazy {
        viewModel.session
    }

    override val screenName = "RequestAmount"
    override val segmentation by lazy { countly.subAccountSegmentation(session, viewModel.getSubAccountLiveData().value) }

    override fun inflate(layoutInflater: LayoutInflater) = RequestAmountLabelBottomSheetBinding.inflate(layoutInflater)

    @Inject
    lateinit var viewModelFactory: RequestAmountLabelViewModel.AssistedFactory
    private val requestViewModel: RequestAmountLabelViewModel by viewModels {
        RequestAmountLabelViewModel.provideFactory(
            viewModelFactory,
            session,
            viewModel.requestAmount.value,
            viewModel.label.value
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = requestViewModel
        binding.amountTextInputLayout.endIconCopyMode()
        binding.labelInputLayout.endIconCopyMode()

        AmountTextWatcher.watch(binding.amountEditText)

        binding.buttonOK.setOnClickListener {
            var amount : String? = null

            try{
                val input = UserInput.parseUserInput(session, requestViewModel.requestAmount.value, isFiat = requestViewModel.isFiat.value ?: false)

                // Convert it to BTC as per BIP21 spec
                amount = input.getBalance(session).let { balance ->
                    if(balance != null && balance.satoshi > 0){
                        balance.btc.let {
                            // Remove trailing zeros if needed
                            if(it.contains(".")) it.replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "") else it
                        }
                    }else{
                        null
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
            }

            viewModel.setRequestAmountAndLabel(amount, requestViewModel.label.value)

            dismiss()
        }

        binding.buttonCurrency.setOnClickListener {
            requestViewModel.toggleCurrency()
        }

        binding.buttonClear.setOnClickListener {
            requestViewModel.requestAmount.value = ""
            viewModel.clearRequestAmountAndLabel()
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