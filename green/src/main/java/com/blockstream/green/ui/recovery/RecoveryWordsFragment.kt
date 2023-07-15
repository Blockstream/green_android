package com.blockstream.green.ui.recovery

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.RecoveryWordsFragmentBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.utils.greenText
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RecoveryWordsFragment : AppFragment<RecoveryWordsFragmentBinding>(
    layout = R.layout.recovery_words_fragment,
    menuRes = 0
) {
    private val args: RecoveryWordsFragmentArgs by navArgs()

    private val networkOrNull by lazy { args.args.network }

    override val title: String
        get() = networkOrNull?.canonicalName ?: ""

    override val toolbarIcon: Int?
        get() = networkOrNull?.getNetworkIcon()

    private val viewModel: RecoveryWordsViewModel by viewModel {
        parametersOf(args.args)
    }

    // If wallet is null, WalletFragment will give the viewModel to AppFragment, guard this behavior and return null
    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun getAppViewModel(): AppViewModelAndroid? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        binding.title.text = greenText(R.string.id_write_down_your_recovery_phrase, R.string.id_recovery_phrase, R.string.id_correct_order)

        binding.buttonNext.setOnClickListener {
            viewModel.postEvent(Events.Continue)
        }
    }

    override fun handleSideEffect(sideEffect: SideEffect) {
        if (sideEffect is SideEffects.NavigateTo) {
            if (sideEffect.destination is NavigateDestinations.RecoveryCheck) {
                val recoveryArgs = (sideEffect.destination as NavigateDestinations.RecoveryCheck).args
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentToRecoveryCheckFragment(
                        args = recoveryArgs,
                    )
                )
            } else if (sideEffect.destination is NavigateDestinations.RecoveryWords) {
                val recoveryArgs = (sideEffect.destination as NavigateDestinations.RecoveryWords).args
                navigate(
                    RecoveryWordsFragmentDirections.actionRecoveryWordsFragmentSelf(
                        args = recoveryArgs
                    )
                )
            }
        } else {
            super.handleSideEffect(sideEffect)
        }
    }
}