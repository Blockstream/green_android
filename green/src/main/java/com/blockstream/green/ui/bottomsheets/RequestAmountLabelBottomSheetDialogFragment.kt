package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.data.Denomination
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.utils.UserInput
import com.blockstream.green.databinding.RequestAmountLabelBottomSheetBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.ui.receive.RequestAmountLabelViewModel
import com.blockstream.green.utils.AmountTextWatcher
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


/*
 * Request Label is hidden at the moment, as we need implementation from GDK
 * to save the address label
 */
class RequestAmountLabelBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RequestAmountLabelBottomSheetBinding, ReceiveViewModel>() {
    override val screenName = "RequestAmount"
    override val segmentation get() = countly.accountSegmentation(session, viewModel.account)

    override fun inflate(layoutInflater: LayoutInflater) = RequestAmountLabelBottomSheetBinding.inflate(layoutInflater)

    private val requestViewModel: RequestAmountLabelViewModel by viewModel {
        parametersOf(
            viewModel.greenWallet,
            viewModel.accountAsset.value!!,
            viewModel.requestAmount.value
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = requestViewModel
        binding.amountTextInputLayout.endIconCustomMode()

        AmountTextWatcher.watch(binding.amountEditText)

        binding.buttonOK.setOnClickListener {
            val amount: String? = try{
                val input = UserInput.parseUserInput(session = session, input = requestViewModel.requestAmount.value, assetId = viewModel.accountAsset.value!!.assetId, denomination = Denomination.defaultOrFiat(session,requestViewModel.isFiat.value ?: false))

                // Convert it to BTC as per BIP21 spec
                runBlocking {
                    input.getBalance().let { balance ->
                        if (balance != null && balance.satoshi > 0) {
                            balance.valueInMainUnit.let {
                                // Remove trailing zeros if needed
                                if (it.contains(".")) it.replace("0*$".toRegex(), "")
                                    .replace("\\.$".toRegex(), "") else it
                            }
                        } else {
                            null
                        }
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
                null
            }

            viewModel.postEvent(ReceiveViewModel.LocalEvents.SetRequestAmount(amount))

            dismiss()
        }

        binding.buttonCurrency.setOnClickListener {
            requestViewModel.toggleCurrency()
        }

        binding.buttonClear.setOnClickListener {
            requestViewModel.requestAmount.value = ""
            viewModel.postEvent(ReceiveViewModel.LocalEvents.SetRequestAmount(null))
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