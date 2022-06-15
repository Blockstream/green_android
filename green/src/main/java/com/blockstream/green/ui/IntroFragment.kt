package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.blockstream.green.R
import com.blockstream.green.databinding.IntroFragmentBinding
import com.blockstream.green.ui.settings.AppSettingsDialogFragment
import com.blockstream.green.utils.errorDialog
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class IntroFragment : WalletListCommonFragment<IntroFragmentBinding>(R.layout.intro_fragment, menuRes = 0) {
    override val screenName = "Home"

    val viewModel: WalletListCommonViewModel by viewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel
        binding.activityVm = activityViewModel

        binding.buttonAppSettings.setOnClickListener {
            AppSettingsDialogFragment.show(childFragmentManager)
        }

        activityViewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                errorDialog(throwable)
            }
        }

        viewModel.onError.observe(viewLifecycleOwner){
            it?.getContentIfNotHandledOrReturnNull()?.let{ throwable ->
                errorDialog(throwable)
            }
        }

        init(binding.common, viewModel)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppActivity).lockDrawer(true)
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as AppActivity).lockDrawer(false)
    }
}