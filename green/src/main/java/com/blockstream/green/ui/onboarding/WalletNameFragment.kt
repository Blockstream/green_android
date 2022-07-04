package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.WalletNameFragmentBinding
import com.blockstream.green.ui.settings.AppSettingsDialogFragment
import com.blockstream.green.utils.dialog
import com.blockstream.green.utils.errorDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletNameFragment : AbstractOnboardingFragment<WalletNameFragmentBinding>(
    R.layout.wallet_name_fragment,
    menuRes = 0
) {
    private val args: WalletNameFragmentArgs by navArgs()

    override val screenName = "OnBoardWalletName"

    override val isAdjustResize: Boolean = true

    @Inject
    lateinit var viewModelFactory: WalletNameViewModel.AssistedFactory
    val viewModel: WalletNameViewModel by viewModels {
        WalletNameViewModel.provideFactory(viewModelFactory, args.onboardingOptions, args.mnemonic, args.mnemonicPassword, args.restoreWallet)
    }

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        options = args.onboardingOptions

        binding.buttonContinue.setOnClickListener {
            options?.let {
                if(it.isRestoreFlow){
                    viewModel.checkRecoveryPhrase(NavigateEvent.Navigate)
                }else{
                    navigateToPin()
                }
            }
        }

        binding.buttonSettings.setOnClickListener {
            AppSettingsDialogFragment.show(childFragmentManager)
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ error ->
                if(error.message == "id_login_failed"){
                    dialog(title = getString(R.string.id_wallet_not_found), message = getString(R.string.id_no_multisig_shield_wallet)) {
                        popBackStack()
                    }
                }else if(error.message?.contains("decrypt_mnemonic") == true){
                    dialog(title = getString(R.string.id_error), message = getString(R.string.id_error_passphrases_do_not_match)) {
                        popBackStack()
                    }
                }else{
                    errorDialog(error) {
                        if(error.message == "id_wallet_already_restored"){
                            popBackStack()
                        }
                    }
                }
            }
        }

        viewModel.onProgress.observe(viewLifecycleOwner){
            setToolbarVisibility(!it)
            onBackCallback.isEnabled = it
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                options?.let { options ->
                    navigate(WalletNameFragmentDirections.actionWalletNameFragmentToOnBoardingCompleteFragment(
                        onboardingOptions = options.copy(walletName = viewModel.getName()),
                        wallet = navigate.data as Wallet)
                    )
                }
            }

            event.getContentIfNotHandledForType<NavigateEvent.Navigate>()?.let {
                navigateToPin()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)
    }

    private fun navigateToPin(){
        options?.copy(walletName = viewModel.getName())?.let {
            navigate(WalletNameFragmentDirections.actionWalletNameFragmentToSetPinFragment(onboardingOptions = it, restoreWallet = args.restoreWallet, mnemonic = args.mnemonic))
        }
    }
}