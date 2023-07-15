package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import breez_sdk.InputType
import breez_sdk.LnUrlWithdrawRequestData
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.lightning.domain
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.UserInput
import com.blockstream.green.R
import com.blockstream.green.databinding.LnurlWithdrawFragmentBinding
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.ui.bottomsheets.DenominationBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.getClipboard
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LnUrlWithdrawFragment : AbstractWalletFragment<LnurlWithdrawFragmentBinding>(
    R.layout.lnurl_withdraw_fragment,
    menuRes = 0
) {
    override val screenName = "LNURLWithdraw"

    val args: LnUrlWithdrawFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    private val requestDataOrNull: LnUrlWithdrawRequestData? by lazy {
        (session.parseInput(
            args.withdraw
        )?.second as? InputType.LnUrlWithdraw)?.data
    }

    private val requestData by lazy { requestDataOrNull!! }

    override val subtitle: String
        get() = wallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    val viewModel: LnUrlWithdrawViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.accountAsset,
            requestData
        )
    }

    override fun getAppViewModel(): AppViewModelAndroid? {
        return if (requestDataOrNull != null) viewModel else null
    }

    override fun getWalletViewModel() = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if(sideEffect is SideEffects.Success){
            dialog(getString(R.string.id_success), getString(R.string.id_s_will_send_you_the_funds_it, requestData.domain() ?: "-")){
                popBackStack()
            }
        }
    }

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        if (requestDataOrNull == null) {
            popBackStack()
            return
        }

        logger.info { requestData }

        binding.vm = viewModel

        binding.redeemFrom = getString(R.string.id_you_are_redeeming_funds_from_s, requestData.domain())

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { throwable ->
                errorDialog(throwable = throwable, errorReport = ErrorReport.create(throwable = throwable, network = session.lightning, session = session)) {
                    popBackStack()
                }
            }
        }

        listOf(binding.buttonAmountCurrency, binding.amountCurrency).setOnClickListener {
            lifecycleScope.launch {
                UserInput.parseUserInputSafe(
                    session,
                    viewModel.amount.value,
                    assetId = viewModel.accountValue.network.policyAsset,
                    denomination = viewModel.denomination.value!!

                ).getBalance().also {
                    DenominationBottomSheetDialogFragment.show(
                        denominatedValue = DenominatedValue(
                            balance = it,
                            assetId = session.lightning?.policyAsset,
                            denomination = viewModel.denomination.value!!
                        ),
                        childFragmentManager)
                }
            }
        }

        binding.buttonAmountPaste.setOnClickListener {
            viewModel.amount.value = getClipboard(requireContext())
        }

        binding.buttonAmountClear.setOnClickListener {
            viewModel.amount.value = ""
        }

        binding.buttonRedeem.setOnClickListener {
            viewModel.withdraw()
            hideKeyboard()
        }
    }
}
