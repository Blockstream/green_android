package com.blockstream.green.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.EnterXpubFragmentBinding
import com.blockstream.green.ui.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.utils.clearNavigationResult
import com.blockstream.green.utils.endIconCopyMode
import com.blockstream.green.utils.getNavigationResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class EnterXpubFragment : WalletFragment<EnterXpubFragmentBinding>(
    layout = R.layout.enter_xpub_fragment,
    menuRes = 0
) {
    override val isAdjustResize = true

    val args: EnterXpubFragmentArgs by navArgs()

    override val wallet by lazy { args.wallet }

    @Inject
    lateinit var viewModelFactory: EnterXpubViewModel.AssistedFactory
    val viewModel: EnterXpubViewModel by viewModels {
        EnterXpubViewModel.provideFactory(viewModelFactory, wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel
        binding.textInputLayout.endIconCopyMode()

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.xpub.postValue(it)
            }
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.open(this)
        }

        binding.buttonContinue.setOnClickListener {
            navigate(EnterXpubFragmentDirections.actionEnterXpubFragmentToAddAccountFragment(
                wallet = args.wallet,
                accountType = args.accountType,
                xpub = viewModel.xpub.value ?: ""
            ))
        }
    }
}