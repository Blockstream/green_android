package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import breez_sdk.InputType
import breez_sdk.LnUrlWithdrawRequestData
import com.blockstream.green.R
import com.blockstream.green.data.DenominatedValue
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.LnurlWithdrawFragmentBinding
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.hideKeyboard
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.bottomsheets.DenominationBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.UserInput
import com.blockstream.green.utils.getClipboard
import com.blockstream.lightning.domain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
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

    @Inject
    lateinit var viewModelFactory: LnUrlWithdrawViewModel.AssistedFactory
    val viewModel: LnUrlWithdrawViewModel by viewModels {
        LnUrlWithdrawViewModel.provideFactory(
            viewModelFactory,
            wallet = args.wallet,
            accountAsset = args.accountAsset,
            requestData = requestData
        )
    }

    override fun getAppViewModel(): AppViewModel? {
        return if (requestDataOrNull != null) viewModel else null
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        if (requestDataOrNull == null) {
            popBackStack()
            return
        }

        logger.info { requestData }

        binding.vm = viewModel

        binding.redeemFrom = getString(R.string.id_you_are_redeeming_funds_from_s, requestData.domain())

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->
            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateBack>()?.let {
                dialog(getString(R.string.id_success), getString(R.string.id_s_will_send_you_the_funds_it, requestData.domain() ?: "-")){
                    popBackStack()
                }
            }
        }

        viewModel.onError.observe(viewLifecycleOwner) {
            it?.getContentIfNotHandledOrReturnNull()?.let { throwable ->
                errorDialog(throwable, showReport = true) {
                    popBackStack()
                }
            }
        }

        listOf(binding.buttonAmountCurrency, binding.amountCurrency).setOnClickListener {
            lifecycleScope.launch {
                UserInput.parseUserInputSafe(
                    session,
                    viewModel.amount.value,
                    assetId = viewModel.account.network.policyAsset,
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
