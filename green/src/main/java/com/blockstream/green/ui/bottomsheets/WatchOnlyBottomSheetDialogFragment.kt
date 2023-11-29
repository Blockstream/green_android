package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.text.trimmedLength
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.WatchOnlyBottomSheetBinding
import com.blockstream.green.extensions.errorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging

class WatchOnlyBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<WatchOnlyBottomSheetBinding, GreenViewModel>() {
    override val screenName = "WatchOnlyCredentials"

    override fun inflate(layoutInflater: LayoutInflater) =
        WatchOnlyBottomSheetBinding.inflate(layoutInflater)

    override val network: Network
        get() = BundleCompat.getParcelable(requireArguments(), NETWORK, Network::class.java)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val watchOnlyUsernameStateFlow = session.watchOnlyUsername(network)

        watchOnlyUsernameStateFlow.value.let {
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
            updateWatchOnly(delete = true)
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            updateWatchOnly()
        }
    }

    private fun updateWatchOnly(delete: Boolean = false) {
        lifecycleScope.launch(context = logException(countly)) {
            try {
                binding.onProgress = true

                withContext(Dispatchers.IO) {
                    viewModel.session.setWatchOnly(
                        network,
                        username = binding.username?.trim().takeIf { !delete } ?: "",
                        password = binding.password?.trim().takeIf { !delete } ?: ""
                    )
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