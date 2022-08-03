package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.EnterXpubFragmentBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
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

    @Inject
    lateinit var viewModelFactory: EnterXpubViewModel.AssistedFactory
    val viewModel: EnterXpubViewModel by viewModels {
        EnterXpubViewModel.provideFactory(viewModelFactory, args.wallet)
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel
        binding.textInputLayout.endIconCustomMode()

        getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.xpub.postValue(it)
            }
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(fragmentManager = childFragmentManager)
        }

        binding.buttonContinue.setOnClickListener {
            navigate(
                EnterXpubFragmentDirections.actionGlobalReviewAddAccountFragment(
                    wallet = args.wallet,
                    assetId = args.assetId,
                    network = args.network,
                    accountType = args.accountType,
                    xpub = viewModel.xpub.value ?: ""
                )
            )
        }
    }
}