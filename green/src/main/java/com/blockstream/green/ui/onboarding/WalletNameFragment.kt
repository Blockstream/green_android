package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.WalletNameFragmentBinding
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.gdk.getGDKErrorCode
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.libgreenaddress.KotlinGDK
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletNameFragment :
    AbstractOnboardingFragment<WalletNameFragmentBinding>(
        R.layout.wallet_name_fragment,
        menuRes = 0
    ) {

    @Inject
    lateinit var viewModelFactory: WalletNameViewModel.AssistedFactory
    val viewModel: WalletNameViewModel by viewModels {
        WalletNameViewModel.provideFactory(viewModelFactory, args.restoreWallet)
    }
    private val args: WalletNameFragmentArgs by navArgs()

    private val onBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Prevent back
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions
        binding.vm = viewModel

        binding.buttonContinue.setOnClickListener {
            options?.let {
                if(it.isHardwareOnboarding()){

                    // This is only available in DEVELOPMENT flavor until development completes
                    if(requireContext().isDevelopmentFlavor()){
                        viewModel.loginWithDevice(it)
                    }

                } else if(it.isRestoreFlow){
                    viewModel.checkRecoveryPhrase(it.network!!, args.mnemonic, args.mnemonicPassword)
                }else{
                    navigateToPin()
                }
            }
        }

        binding.buttonSettings.setOnClickListener {
            navigate(WalletNameFragmentDirections.actionGlobalConnectionSettingsDialogFragment())
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                errorDialog(getString(if (throwable.getGDKErrorCode() == KotlinGDK.GA_ERROR) R.string.id_login_failed else R.string.id_connection_failed))
            }
        }

        viewModel.onProgress.observe(viewLifecycleOwner){
            setToolbarVisibility(!it)
            onBackCallback.isEnabled = it
        }

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                if(it is Wallet){
                    options?.let { options ->
                        navigate(WalletNameFragmentDirections.actionWalletNameFragmentToOnBoardingCompleteFragment(
                            onboardingOptions = options.copy(walletName = viewModel.getName()),
                            wallet = it)
                        )
                    }

                }else {
                    navigateToPin()
                }
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