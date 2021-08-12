package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseSecurityFragmentBinding
import com.blockstream.green.ui.ComingSoonBottomSheetDialogFragment
import com.blockstream.green.ui.HelpBottomSheetDialogFragment
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
                    navigate(createCopyForNetwork(greenWallet, networkType!!, true))
                }
            }else{
                ComingSoonBottomSheetDialogFragment().also {
                    it.show(childFragmentManager, it.toString())
                }
            }
        }

        binding.multiSig.setOnClickListener {
            options?.apply {
                navigate(createCopyForNetwork(greenWallet, networkType!!, false))
            }
        }
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
            navigate(
                ChooseSecurityFragmentDirections.actionChooseSecurityFragmentToRecoveryIntroFragment(
                    wallet = null,
                    onboardingOptions = options,
                    mnemonic = greenWallet.generateMnemonic12()
                ), navOptionsBuilder = navOptionsBuilder
            )
        }
    }
}