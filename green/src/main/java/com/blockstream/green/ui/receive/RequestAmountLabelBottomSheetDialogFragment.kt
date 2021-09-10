package com.blockstream.green.ui.receive

import android.os.Bundle
import android.view.View
import com.blockstream.gdk.params.Convert
import com.blockstream.green.R
import com.blockstream.green.databinding.RequestAmountLabelBottomSheetBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


/*
 * Request Label is hidden at the moment, as we need implementation from GDK
 * to save the address label
 */
@AndroidEntryPoint
class RequestAmountLabelBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RequestAmountLabelBottomSheetBinding>(
    layout = R.layout.request_amount_label_bottom_sheet
) {
    var isFiat = false

    lateinit var receiveViewModel: ReceiveViewModel
    lateinit var session: GreenSession

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveViewModel = (parentFragment as ReceiveFragment).viewModel
        session = viewModel.session

        // Init label from Receive
        binding.label = receiveViewModel.label.value
        // Init amount from Receive
        binding.amount = receiveViewModel.requestAmount.value.let { amount ->
            if(!amount.isNullOrBlank()){
                try {
                    // Amount is always in BTC value, convert it to user's settings
                    session
                        .convertAmount(Convert.forUnit(session.network.policyAsset, amount)).btc(session, withUnit = false)
                }catch (e: Exception){
                    e.printStackTrace()
                    amount
                }
            }else{
                amount
            }
        }

        AmountTextWatcher.watch(binding.amountEditText)

        binding.buttonOK.setOnClickListener {
            var amount : String? = null

            try{
                val input = UserInput.parseUserInput(session, binding.amount, isFiat = isFiat)

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

            (parentFragment as ReceiveFragment).viewModel.setRequestAmountAndLabel(amount, binding.label)

            dismiss()
        }

        binding.buttonCurrency.setOnClickListener {

            // Convert between BTC / Fiat
            binding.amount = try{
                val input = UserInput.parseUserInput(session, binding.amount, isFiat = isFiat)
                input.getBalance(session).let {
                    if(it != null && it.satoshi > 0){
                        it.getValue(if(isFiat) getUnit(session) else getFiatCurrency(session))
                    }else{
                        ""
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
                ""
            }

            isFiat = !isFiat
            updateCurrency()
        }

        binding.buttonClear.setOnClickListener {
            (parentFragment as ReceiveFragment).clearRequestAmountAndLabel()
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        updateCurrency()
    }

    private fun updateCurrency(){
        (if(isFiat) getFiatCurrency(session) else getBitcoinOrLiquidUnit(session)).let {
            binding.amountTextInputLayout.helperText = getString(R.string.id_amount_in_s, it)
            binding.symbol = it
        }
    }
}