package com.blockstream.green.ui.lightning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.lightning.RecoverFundsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.lightning.LnUrlAuthScreen
import com.blockstream.compose.screens.lightning.RecoverFundsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.RecoverFundsFragmentBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.dialog
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.filters.NumberValueFilter
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.AccountAssetBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import com.blockstream.green.utils.getClipboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.text.NumberFormat

class RecoverFundsFragment : AppFragment<ComposeViewBinding>(
    R.layout.compose_view,
    menuRes = 0
) {
    val args: RecoverFundsFragmentArgs by navArgs()

    override val title: String
        get() = getString(if (viewModel.isRefund) R.string.id_refund else if(viewModel.isSendAll) R.string.id_empty_lightning_account else R.string.id_sweep)

    override val subtitle: String
        get() = viewModel.greenWallet.name

    override val toolbarIcon: Int
        get() = R.drawable.ic_lightning

    val viewModel: RecoverFundsViewModel by viewModel {
        parametersOf(
            args.wallet,
            args.isSendAll,
            args.address,
            args.amount
        )
    }

    override fun getGreenViewModel() = viewModel

    override val useCompose: Boolean = true
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppFragmentBridge {
                    RecoverFundsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
