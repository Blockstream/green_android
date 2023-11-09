package com.blockstream.green.ui.add

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.add.XpubViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.XpubFragmentBinding
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf



class XpubFragment : AbstractAddAccountFragment<XpubFragmentBinding>(
    layout = R.layout.xpub_fragment,
    menuRes = 0
) {
    override val isAdjustResize = true

    val args: XpubFragmentArgs by navArgs()

    override val title: String?
        get() = args.setupArgs.network?.canonicalName

    override val toolbarIcon: Int?
        get() = args.setupArgs.network?.getNetworkIcon()

    override val assetId: String?
        get() = args.setupArgs.assetId


    override val viewModel: XpubViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if(sideEffect is SideEffects.Navigate){
            (sideEffect.data as? SetupArgs)?.also {
                navigate(
                    XpubFragmentDirections.actionGlobalReviewAddAccountFragment(
                        setupArgs = it
                    )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.textInputLayout.endIconCustomMode()

        getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(viewLifecycleOwner) { result ->
            result?.let {
                clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                viewModel.xpub.value = it.result
            }
        }

        binding.buttonScan.setOnClickListener {
            CameraBottomSheetDialogFragment.showSingle(screenName = screenName, fragmentManager = childFragmentManager)
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }
    }
}