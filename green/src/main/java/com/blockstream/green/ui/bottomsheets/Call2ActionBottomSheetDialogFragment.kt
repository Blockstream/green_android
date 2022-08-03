package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.Call2ActionBottomSheetBinding
import com.blockstream.green.extensions.navigate
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class Call2ActionBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<Call2ActionBottomSheetBinding, AbstractWalletViewModel>() {

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) =
        Call2ActionBottomSheetBinding.inflate(layoutInflater)

    override val accountOrNull: Account?
        get() = requireArguments().getParcelable(ACCOUNT)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEnable2fa: Boolean = requireArguments().getBoolean(ENABLE_2FA)
        val isIncreaseSecurity: Boolean = requireArguments().getBoolean(INCREASE_SECURITY)

        if(isEnable2fa){
            binding.title = getString(R.string.id_enable_2fa)
            binding.message = getString(R.string.id_2fa_isnt_set_up_yetnnyou_can)
            binding.button = getString(R.string.id_setup_2fa_now)
        }else if(isIncreaseSecurity){
            binding.title = getString(R.string.id_increase_security)
            binding.message = getString(R.string.id_your_funds_have_grown)
            binding.button = getString(R.string.id_add_2fa_account)
        }

        binding.buttonAction.setOnClickListener {
            if(isEnable2fa) {
                NavGraphDirections.actionGlobalTwoFractorAuthenticationFragment(
                    wallet = wallet,
                    network = account.network
                ).also {
                    navigate(findNavController(), it.actionId, it.arguments)
                }
            }else if(isIncreaseSecurity){
                NavGraphDirections.actionGlobalReviewAddAccountFragment(
                    wallet = wallet,
                    assetId = account.network.policyAsset,
                    network = account.network,
                    accountType = AccountType.STANDARD
                ).also {
                    navigate(findNavController(), it.actionId, it.arguments)
                }
            }

            dismiss()
        }
    }

    companion object : KLogging() {
        private const val ACCOUNT = "ACCOUNT"
        private const val ENABLE_2FA = "ENABLE_2FA"
        private const val INCREASE_SECURITY = "INCREASE_SECURITY"

        fun showEnable2FA(account: Account, fragmentManager: FragmentManager) {
            show(Call2ActionBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putBoolean(ENABLE_2FA, true)
                    bundle.putParcelable(ACCOUNT, account)
                }
            }, fragmentManager)
        }

        fun showIncreaseSecurity(account: Account, fragmentManager: FragmentManager) {
            show(Call2ActionBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putBoolean(INCREASE_SECURITY, true)
                    bundle.putParcelable(ACCOUNT, account)
                }
            }, fragmentManager)
        }
    }


}