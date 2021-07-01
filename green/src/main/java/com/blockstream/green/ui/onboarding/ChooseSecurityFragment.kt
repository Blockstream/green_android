package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseSecurityFragmentBinding
import com.blockstream.green.utils.isProductionFlavor
import com.blockstream.green.utils.notifyDevelopmentFeature
import com.greenaddress.Bridge
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        val isSinglesigNetworkEnabledForBuildFlavor =
            options!!.isSinglesigNetworkEnabledForBuildFlavor(requireContext())

        binding.singleSig.setOnClickListener {
            if (isSinglesigNetworkEnabledForBuildFlavor) {
                options?.apply {
                    navigate(copy(network = getNetwork(networkType!!, true)))
                }
            }
        }

        binding.multiSig.setOnClickListener {
            options?.apply {
                navigate(copy(network = getNetwork(networkType!!, false)))
            }
        }


        if (!isSinglesigNetworkEnabledForBuildFlavor) {
            // Disable singlesig
            binding.singleSig.disable()

            // Or proceed immediately to next destination
            options?.apply {
                // Remove chooseSecurityFragment from backstack
                val navOptionsBuilder =
                    NavOptions.Builder().setPopUpTo(R.id.chooseSecurityFragment, true)
                navigate(copy(network = getNetwork(networkType!!, false)), navOptionsBuilder)
            }
        }
    }

    private fun getNetwork(networkType: String, isElectrum: Boolean): Network {
        val id = when (networkType) {
            "mainnet" -> {
                if (isElectrum) "electrum-mainnet" else "mainnet"
            }
            "liquid" -> {
                if (isElectrum) "electrum-liquid" else "liquid"
            }
            else -> {
                if (isElectrum) "electrum-testnet" else "testnet"
            }
        }

        return greenWallet.networks.getNetworkById(id)
    }

    private fun navigate(
        options: OnboardingOptions,
        navOptionsBuilder: NavOptions.Builder? = null
    ) {
        if (options.isRestoreFlow) {
            navigate(
                ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToChooseRecoveryPhraseFragment(
                    options
                ),
                navOptionsBuilder = navOptionsBuilder
            )
        } else {
            if (Bridge.useGreenModule) {
                navigate(
                    ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToWalletNameFragment(
                        options,
                        mnemonic = "",
                        mnemonicPassword = ""
                    ), navOptionsBuilder = navOptionsBuilder
                )
            } else {
                navigate(
                    ChooseSecurityFragmentDirections.actionGlobalRecoveryIntroFragment(
                        wallet = null,
                        onboardingOptions = options,
                        mnemonic = greenWallet.generateMnemonic12()
                    ), navOptionsBuilder = navOptionsBuilder
                )
            }
        }
    }
}