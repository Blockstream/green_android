package com.blockstream.green.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.databinding.ArchiveAccountDialogBinding
import com.blockstream.green.extensions.navigate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging

class ArchiveAccountDialogFragment : AbstractDialogFragment<ArchiveAccountDialogBinding, GreenViewModel>() {
    override val viewModel: GreenViewModel? = null
    override fun inflate(layoutInflater: LayoutInflater): ArchiveAccountDialogBinding =
        ArchiveAccountDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonPrimary.setOnClickListener {
            dismiss()
        }

        appFragment
            ?.getGreenViewModel()
            ?.sessionOrNull
            ?.allAccounts
            ?.onEach { accounts ->
                binding.archivedAccounts = accounts.count { it.hidden }
            }?.launchIn(lifecycleScope)

        binding.buttonSecondary.setOnClickListener {
            appFragment?.getGreenViewModel()?.greenWalletOrNull?.also { wallet ->
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
