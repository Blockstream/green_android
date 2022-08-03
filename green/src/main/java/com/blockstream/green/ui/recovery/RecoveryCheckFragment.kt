package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.RecoveryCheckFragmentBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.extensions.snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryCheckFragment : AbstractWalletFragment<RecoveryCheckFragmentBinding>(
    layout = R.layout.recovery_check_fragment,
    menuRes = 0
) {
    private val args: RecoveryCheckFragmentArgs by navArgs()

    override val screenName = "RecoveryCheck"

    override val walletOrNull by lazy { args.wallet }
    private val networkOrNull by lazy { args.network }

    override val title: String
        get() = networkOrNull?.canonicalName ?: ""

    override val toolbarIcon: Int?
        get() = networkOrNull?.getNetworkIcon()

    @Inject
    lateinit var viewModelFactory: RecoveryCheckViewModel.AssistedFactory

    @Inject
    lateinit var gdkBridge: GdkBridge

    private val viewModel: RecoveryCheckViewModel by viewModels {
        RecoveryCheckViewModel.provideFactory(
            viewModelFactory,
            args.mnemonic.split(" "),
            args.page
        )
    }

    @Inject
    lateinit var walletViewModelFactory: WalletViewModel.AssistedFactory
    val walletViewModel: WalletViewModel by viewModels {
        WalletViewModel.provideFactory(walletViewModelFactory, args.wallet!!)
    }

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getAppViewModel() : AppViewModel? = if(args.wallet == null) null else getWalletViewModel()

    override fun getWalletViewModel(): AbstractWalletViewModel = if(args.wallet != null) walletViewModel else throw RuntimeException("Can't be happening")

    // Recovery screens are reused in onboarding
    // where we don't have a session yet.
    override fun isSessionAndWalletRequired(): Boolean {
        return args.wallet != null
    }

    override fun isLoggedInRequired(): Boolean = isSessionAndWalletRequired()

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel
        binding.isDevelopmentFlavor = isDevelopmentFlavor

        viewModel.onEvent.observe(viewLifecycleOwner) { consumableEvent ->

            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.Navigate>()?.let {
                if (viewModel.isLastPage) {
                    if(args.wallet == null) {
                        navigate(
                            RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentToSetPinFragment(
                                restoreWallet = null,
                                onboardingOptions = args.onboardingOptions!!,
                                mnemonic = args.mnemonic,
                                password = ""
                            ), navOptionsBuilder = NavOptions.Builder().also {
                                it.setPopUpTo(R.id.recoveryIntroFragment, false)
                            }
                        )
                    }else{
                        navigate(
                            RecoveryCheckFragmentDirections.actionGlobalReviewAddAccountFragment(
                                wallet = args.wallet!!,
                                assetId = args.assetId!!,
                                network = args.network!!,
                                accountType = AccountType.TWO_OF_THREE,
                                mnemonic = args.mnemonic,
                            ), navOptionsBuilder = NavOptions.Builder().also {
                                it.setPopUpTo(R.id.recoveryIntroFragment, false)
                            }
                        )
                    }
                } else {
                    navigate(
                        RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentSelf(
                            wallet = args.wallet,
                            assetId = args.assetId,
                            network = args.network,
                            onboardingOptions = args.onboardingOptions,
                            mnemonic = args.mnemonic,
                            page = args.page + 1
                        ), navOptionsBuilder = NavOptions.Builder().also {
                            it.setPopUpTo(R.id.recoveryIntroFragment, false)
                        }
                    )
                }
            }

            consumableEvent?.getContentIfNotHandledForType<NavigateEvent.NavigateBack>()?.let {
                snackbar(R.string.id_wrong_choice_check_your)
                findNavController().popBackStack(R.id.recoveryIntroFragment, false)
            }
        }

        binding.clickListener = View.OnClickListener { button ->
            viewModel.selectWord((button as Button).text.toString())
        }
    }
}