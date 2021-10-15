package com.blockstream.green.ui.receive

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.params.Convert
import com.blockstream.green.R
import com.blockstream.green.databinding.RequestAmountLabelBottomSheetBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.ui.looks.AssetLook
import com.blockstream.green.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


/*
 * Request Label is hidden at the moment, as we need implementation from GDK
 * to save the address label
 */
@AndroidEntryPoint
class RequestAmountLabelBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RequestAmountLabelBottomSheetBinding, ReceiveViewModel>(
    layout = R.layout.request_amount_label_bottom_sheet
) {
    val session by lazy {
        viewModel.session
    }

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
            viewModel.clearRequestAmountAndLabel()
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
}