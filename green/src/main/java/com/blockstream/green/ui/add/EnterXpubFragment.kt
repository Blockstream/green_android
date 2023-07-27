package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.EnterXpubFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf



class EnterXpubFragment : AbstractWalletFragment<EnterXpubFragmentBinding>(
    layout = R.layout.enter_xpub_fragment,
    menuRes = 0
) {
    override val isAdjustResize = true

    val args: EnterXpubFragmentArgs by navArgs()

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "AddAccountPublicKey"

    override val title: String
        get() = args.network.canonicalName

    override val toolbarIcon: Int
        get() = args.network.getNetworkIcon()


    val viewModel: EnterXpubViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel
        binding.textInputLayout.endIconCustomMode()

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.xpub.postValue(it.result)
            }
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, fragmentManager = childFragmentManager)
        }

        binding.buttonContinue.setOnClickListener {
            navigate(
                EnterXpubFragmentDirections.actionGlobalReviewAddAccountFragment(
                    setupArgs = SetupArgs(xpub = viewModel.xpub.value ?: ""),
                )
            )
        }
    }
}