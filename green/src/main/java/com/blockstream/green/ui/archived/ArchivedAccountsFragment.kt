package com.blockstream.green.ui.archived


import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.AppFragmentBridge
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.archived.ArchivedAccountsScreen
import com.blockstream.green.R
import com.blockstream.green.databinding.BaseRecyclerViewBinding
import com.blockstream.green.databinding.ComposeViewBinding
import com.blockstream.green.extensions.showPopupMenu
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.RenameAccountBottomSheetDialogFragment
import com.blockstream.green.ui.items.AccountListItem
import com.blockstream.green.ui.items.TextListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.observeList
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ArchivedAccountsFragment :
    AppFragment<ComposeViewBinding>(R.layout.compose_view, 0) {
    val args: ArchivedAccountsFragmentArgs by navArgs()

    val viewModel: ArchivedAccountsViewModel by viewModel {
        parametersOf(args.wallet, args.navigateToOverview)
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
                    ArchivedAccountsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
