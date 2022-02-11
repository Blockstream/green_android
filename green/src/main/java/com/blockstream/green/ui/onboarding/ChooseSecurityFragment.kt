package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseSecurityFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChooseSecurityFragment :
    AbstractOnboardingFragment<ChooseSecurityFragmentBinding>(
        R.layout.choose_security_fragment,
        menuRes = 0
    ) {

    @Inject
    lateinit var greenWallet: GreenWallet

    private val args: ChooseSecurityFragmentArgs by navArgs()

    override val screenName = "OnBoardChooseSecurity"

    @Inject
    lateinit var viewModelFactory: ChooseSecurityViewModel.AssistedFactory
    val viewModel: ChooseSecurityViewModel by viewModels {
        ChooseSecurityViewModel.provideFactory(viewModelFactory, args.onboardingOptions, args.isManualRestore)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        binding.vm = viewModel

        binding.singleSig.setOnClickListener {
            options?.apply {
                navigate(
                    createCopyForNetwork(
                        greenWallet = greenWallet,
                        networkType = networkType!!,
                        isElectrum = true,
                    )
                )
            }
        }

        binding.multiSig.setOnClickListener {
            options?.apply {
                navigate(
                    createCopyForNetwork(
                        greenWallet = greenWallet,
                        networkType = networkType!!,
                        isElectrum = false,
                    )
                )
            }
        }

        binding.toggleRecoverySize.addOnButtonCheckedListener { _, checkedId, _ ->
            viewModel.recoverySize.value = checkedId
        }

        binding.toggleRecoverySize.check(viewModel.recoverySize.value ?: R.id.button12)
    }

    private fun navigate(
        options: OnboardingOptions,
        navOptionsBuilder: NavOptions.Builder? = null
    ) {
        if (options.isRestoreFlow) {
            navigate(
                ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToWalletNameFragment(
                    onboardingOptions = options,
                    mnemonic = args.mnemonic ?: "",
                    mnemonicPassword = args.mnemonicPassword ?: ""
                ),
                navOptionsBuilder = navOptionsBuilder
            )
        } else {
            val mnemonic = if(viewModel.recoverySize.value == R.id.button12) greenWallet.generateMnemonic12() else greenWallet.generateMnemonic24()
            navigate(
                ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToRecoveryIntroFragment(
                    wallet = null,
                    onboardingOptions = options,
                    mnemonic = mnemonic
                ), navOptionsBuilder = navOptionsBuilder
            )
        }
    }
}