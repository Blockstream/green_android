package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Events
import com.blockstream.common.lightning.domain
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.LnurlWithdrawFragmentBinding
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.utils.getClipboard
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LnUrlWithdrawFragment : AppFragment<LnurlWithdrawFragmentBinding>(
    R.layout.lnurl_withdraw_fragment,
    menuRes = 0
) {

    val args: LnUrlWithdrawFragmentArgs by navArgs()

    val requestData
        get() = args.lnUrlWithdrawRequest.requestData

    override val subtitle: String
        get() = viewModel.greenWallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    val viewModel: LnUrlWithdrawViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.lnUrlWithdrawRequest.requestData
        )
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Success){
            dialog(getString(R.string.id_success), getString(R.string.id_s_will_send_you_the_funds_it, requestData.domain())){
                popBackStack()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        listOf(binding.buttonAmountCurrency, binding.amountCurrency).setOnClickListener {
            if (!viewModel.onProgress.value) {
                viewModel.postEvent(Events.SelectDenomination)
            }
        }

        binding.buttonAmountPaste.setOnClickListener {
            viewModel.amount.value = getClipboard(requireContext()) ?: ""
        }

        binding.buttonAmountClear.setOnClickListener {
            viewModel.amount.value = ""
        }

        binding.buttonRedeem.setOnClickListener {
            viewModel.postEvent(Events.Continue)
            hideKeyboard()
        }
    }
}
