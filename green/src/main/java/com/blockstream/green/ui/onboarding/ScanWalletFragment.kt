package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ScanWalletFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ScanWalletFragment :
    AbstractOnboardingFragment<ScanWalletFragmentBinding>(
        R.layout.scan_wallet_fragment,
        menuRes = 0
    ) {

    @Inject
    lateinit var greenWallet: GreenWallet

    private val args: ScanWalletFragmentArgs by navArgs()

    override val screenName = "OnBoardScan"

    @Inject
    lateinit var viewModelFactory: ScanWalletViewModel.AssistedFactory
    val viewModel: ScanWalletViewModel by viewModels {
        ScanWalletViewModel.provideFactory(
            viewModelFactory,
            args.onboardingOptions,
            args.mnemonic
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        options = args.onboardingOptions
        binding.vm = viewModel

        viewModel.singleSig.observe(viewLifecycleOwner){
            binding.singleSig.alpha = if(it == "") 1.0f else 0.6f
            binding.singleSig.isEnabled = it == ""

            binding.singleSig.setCaption(when(it){
                "" -> getString(R.string.id_wallet_found)
                null -> getString(R.string.id_wallet_not_found)
                else -> getString(R.string.id_wallet_already_restored) + "\n$it"
            })
        }

        viewModel.multiSig.observe(viewLifecycleOwner){
            binding.multiSig.alpha = if(it == "") 1.0f else 0.6f
            binding.multiSig.isEnabled = it == ""

            binding.multiSig.setCaption(when(it){
                "" -> getString(R.string.id_wallet_found)
                null -> getString(R.string.id_wallet_not_found)
                else -> getString(R.string.id_wallet_already_restored) + "\n$it"
            })
        }

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

        binding.buttonManualRestore.setOnClickListener {
            navigate(ScanWalletFragmentDirections.actionScanWalletFragmentToChooseSecurityFragment(
                onboardingOptions = options!!,
                mnemonic = args.mnemonic,
                mnemonicPassword = args.mnemonicPassword,
                isManualRestore = true
            ))
        }
    }

    private fun navigate(
        options: OnboardingOptions,
    ) {
        navigate(
            ScanWalletFragmentDirections.actionScanWalletFragmentToWalletNameFragment(
                onboardingOptions = options,
                mnemonic = args.mnemonic,
                mnemonicPassword = args.mnemonicPassword
            )
        )
    }

}