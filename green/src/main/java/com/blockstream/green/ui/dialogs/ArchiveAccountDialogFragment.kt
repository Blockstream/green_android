package com.blockstream.green.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.databinding.ArchiveAccountDialogBinding
import com.blockstream.green.extensions.navigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging

@AndroidEntryPoint
class ArchiveAccountDialogFragment : AbstractDialogFragment<ArchiveAccountDialogBinding>() {

    override fun inflate(layoutInflater: LayoutInflater): ArchiveAccountDialogBinding =
        ArchiveAccountDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonPrimary.setOnClickListener {
            dismiss()
        }

        walletFragment?.session
            ?.allAccountsFlow
            ?.onEach { accounts ->
                binding.archivedAccounts = accounts.count { it.hidden }
            }?.launchIn(lifecycleScope)

        binding.buttonSecondary.setOnClickListener {
            walletFragment?.walletOrNull?.also { wallet ->
                NavGraphDirections.actionGlobalArchivedAccountsFragment(
                    wallet = wallet
                ).also {
                    navigate(
                        findNavController(), it.actionId, it.arguments
                    )
                }
            }
            dismiss()
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager) {
            showSingle(ArchiveAccountDialogFragment(), fragmentManager)
        }
    }
}
