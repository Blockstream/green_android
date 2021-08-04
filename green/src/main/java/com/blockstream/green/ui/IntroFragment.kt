package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.databinding.IntroFragmentBinding
import com.blockstream.green.utils.AppKeystore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment : WalletListCommonFragment<IntroFragmentBinding>(R.layout.intro_fragment, menuRes = 0){

    @Inject
    lateinit var appKeystore: AppKeystore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

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