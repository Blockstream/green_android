package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.EditTextDialogBinding
import com.blockstream.green.databinding.IntroFragmentBinding
import com.blockstream.green.databinding.PasswordTextDialogBinding
import com.blockstream.green.databinding.PinTextDialogBinding
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.AppKeystore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment : WalletListCommonFragment<IntroFragmentBinding>(R.layout.intro_fragment, menuRes = 0){

    @Inject
    lateinit var appKeystore: AppKeystore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

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