package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.*
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.onboarding.ChooseRecoveryPhraseFragmentDirections
import com.blockstream.green.ui.wallet.RenameWalletBottomSheetDialogFragment
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.clearNavigationResult
import com.blockstream.green.utils.getNavigationResult
import com.blockstream.green.utils.snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@AndroidEntryPoint
class IntroFragment : WalletListCommonFragment<IntroFragmentBinding>(R.layout.intro_fragment, menuRes = 0) {

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