package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryCheckFragmentBinding
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.utils.isDevelopmentFlavor
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryCheckFragment : AppFragment<RecoveryCheckFragmentBinding>(
    layout = R.layout.recovery_check_fragment,
    menuRes = 0
) {
    private val args: RecoveryCheckFragmentArgs by navArgs()

    private val networkOrNull by lazy { args.args.network }

    override val title: String
        get() = networkOrNull?.canonicalName ?: ""

    override val toolbarIcon: Int?
        get() = networkOrNull?.getNetworkIcon()

    private val viewModel: RecoveryCheckViewModel by viewModel {
        parametersOf(args.args)
    }

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getGreenViewModel(): GreenViewModel = viewModel
    override fun getAppViewModel() : AppViewModelAndroid? = null

    override fun handleSideEffect(sideEffect: SideEffect) {
        if (sideEffect is SideEffects.NavigateTo) {
            if (sideEffect.destination is NavigateDestinations.RecoveryCheck) {
                navigate(
                    RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentSelf(
                        args = (sideEffect.destination as NavigateDestinations.RecoveryCheck).args,
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, false)
                    }
                )
            } else if (sideEffect.destination is NavigateDestinations.SetPin) {
                val recoveryArgs = (sideEffect.destination as NavigateDestinations.SetPin).args
                navigate(
                    RecoveryCheckFragmentDirections.actionRecoveryCheckFragmentToPinFragment(
                        setupArgs = recoveryArgs
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, false)
                    }
                )
            } else if (sideEffect.destination is NavigateDestinations.AddAccount) {
                val recoveryArgs = (sideEffect.destination as NavigateDestinations.AddAccount).args
                navigate(
                    RecoveryCheckFragmentDirections.actionGlobalReviewAddAccountFragment(
                        setupArgs = recoveryArgs
                    ), navOptionsBuilder = NavOptions.Builder().also {
                        it.setPopUpTo(R.id.recoveryIntroFragment, false)
                    }
                )
            }
        } else if (sideEffect is SideEffects.NavigateBack) {
            snackbar(R.string.id_wrong_choice_check_your)
            findNavController().popBackStack(R.id.recoveryIntroFragment, false)
        } else {
            super.handleSideEffect(sideEffect)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.isDevelopmentFlavor = isDevelopmentFlavor

        binding.clickListener = View.OnClickListener { button ->
            viewModel.postEvent(RecoveryCheckViewModel.LocalEvents.SelectWord((button as Button).text.toString()))
        }
    }
}