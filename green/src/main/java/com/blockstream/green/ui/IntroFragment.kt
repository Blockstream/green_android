package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroFragment : WalletListCommonFragment<IntroFragmentBinding>(R.layout.intro_fragment, menuRes = 0) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = activityViewModel

        binding.buttonAppSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalAppSettingsDialogFragment())
        }

        init(binding.common)
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