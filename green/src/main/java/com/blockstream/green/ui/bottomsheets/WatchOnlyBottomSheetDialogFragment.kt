package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.trimmedLength
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.R
import com.blockstream.green.databinding.WatchOnlyBottomSheetBinding
import com.blockstream.green.ui.settings.WalletSettingsFragment
import com.blockstream.green.ui.settings.WalletSettingsViewModel
import com.blockstream.green.utils.errorDialog
import com.blockstream.green.utils.logException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging

@AndroidEntryPoint
class WatchOnlyBottomSheetDialogFragment :
    AbstractBottomSheetDialogFragment<WatchOnlyBottomSheetBinding>() {
    override val screenName = "WatchOnlyCredentials"

    val viewModel: WalletSettingsViewModel by lazy {
        (parentFragment as WalletSettingsFragment).viewModel
    }

    override fun inflate(layoutInflater: LayoutInflater) =
        WatchOnlyBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.hasWatchOnlyCredentials = !viewModel.watchOnlyUsernameLiveData.value.isNullOrBlank()

        binding.username = viewModel.watchOnlyUsernameLiveData.value

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
            viewModel.setWatchOnly(username = "", password = "")
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
                        username = binding.username?.trim() ?: "",
                        password = binding.password?.trim() ?: ""
                    )
                    viewModel.updateWatchOnlyUsername()
                }

                dismiss()
            } catch (e: Exception) {
                errorDialog(e)
            }

            binding.onProgress = false
        }
    }

    companion object : KLogging() {

        fun show(fragmentManager: FragmentManager) {
            show(WatchOnlyBottomSheetDialogFragment(), fragmentManager)
        }
    }
}