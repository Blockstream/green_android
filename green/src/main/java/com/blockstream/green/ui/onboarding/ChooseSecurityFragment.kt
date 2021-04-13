package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseSecurityFragmentBinding
import com.blockstream.green.utils.isProductionFlavor
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

        binding.singleSig.setOnClickListener {
            options?.apply {
                navigate(copy(network = getNetwork(networkType!!, true)))
            }
        }

        binding.multiSig.setOnClickListener {
            options?.apply {
                navigate(copy(network = getNetwork(networkType!!, false)))
            }
        }

        // Skip singlesig, check AppFragment where we remove this fragment from the backstack
        if(requireContext().isProductionFlavor()){
            options?.apply {
                navigate(copy(network = getNetwork(networkType!!, false)))
            }
        }
    }

    private fun getNetwork(networkType: String, isElectrum: Boolean): Network {
        val id = when(networkType){
            "mainnet" -> {
                if(isElectrum) "electrum-mainnet" else "mainnet"
            }
            "liquid" -> {
                if(isElectrum) "liquid-electrum-mainnet" else "liquid"
            }
            else -> {
                if(isElectrum) "electrum-testnet" else "testnet"
            }
        }

        return greenWallet.networks.getNetworkById(id)
    }

    private fun navigate(options: OnboardingOptions) {
        if (options.isRestoreFlow) {
            navigate(
                ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToChooseRecoveryPhraseFragment(
                    options
                )
            )
        } else {
            if(Bridge.useGreenModule){
                navigate(ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToWalletNameFragment(options, mnemonic = "", mnemonicPassword = ""))
            }else{
                navigate(ChooseSecurityFragmentDirections.actionGlobalRecoveryIntroFragment(wallet = null, onboardingOptions = options, mnemonic = greenWallet.generateMnemonic12()))
            }
        }
    }
}