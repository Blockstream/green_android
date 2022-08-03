package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.trimmedLength
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.databinding.WatchOnlyBottomSheetBinding
import com.blockstream.green.ui.settings.WalletSettingsViewModel
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.logException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging

@AndroidEntryPoint
class WatchOnlyBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<WatchOnlyBottomSheetBinding, WalletSettingsViewModel>() {
    override val screenName = "WatchOnlyCredentials"

    override fun inflate(layoutInflater: LayoutInflater) =
        WatchOnlyBottomSheetBinding.inflate(layoutInflater)

    override val network: Network
        get() = requireArguments().getParcelable(NETWORK)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val watchOnlyUsernameLiveData = viewModel.watchOnlyUsernameLiveData(network)

        watchOnlyUsernameLiveData.value.let {
            binding.username = it
            binding.hasWatchOnlyCredentials = !it.isNullOrBlank()
        }

        binding.usernameTextInputEditText.doOnTextChanged { text, _, _, _ ->
            binding.usernameTextInputLayout.error =
                if (text.isNullOrBlank() || text.trimmedLength() >= 8) null else getString(R.string.id_at_least_8_characters_required)
        }

        binding.passwordTextInputEditText.doOnTextChanged { text, _, _, _ ->
            binding.passwordTextInputLayout.error =
                if (text.isNullOrBlank() || text.trimmedLength() >= 8) null else getString(R.string.id_at_least_8_characters_required)
        }

        binding.buttonDelete.setOnClickListener {
            binding.isDeleteConfirmed = true
        }

        binding.buttonDeleteConfirm.setOnClickListener {
            viewModel.setWatchOnly(network = network, username = "", password = "")
            dismiss()
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            setWatchOnly()
        }
    }

    private fun setWatchOnly() {
        lifecycleScope.launch(context = logException(countly)) {
            try {
                binding.onProgress = true

                withContext(Dispatchers.IO) {
                    viewModel.session.setWatchOnly(
                        network,
                        username = binding.username?.trim() ?: "",
                        password = binding.password?.trim() ?: ""
                    )
                    viewModel.updateWatchOnlyUsername()
                }
            } catch (e: Exception) {
                errorDialog(e)
            }finally {
                dismiss()
            }

            binding.onProgress = false
        }
    }

    companion object : KLogging() {

        private const val NETWORK = "NETWORK"

        fun show(network: Network, fragmentManager: FragmentManager) {
            show(WatchOnlyBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(NETWORK, network)
                }
            }, fragmentManager)
        }
    }
}