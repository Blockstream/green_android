package com.blockstream.green.ui.overview

import android.content.Context
import androidx.lifecycle.lifecycleScope
import breez_sdk.InputType
import com.blockstream.common.AddressInputType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.toSerializable
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import kotlinx.coroutines.launch

interface OverviewInterface {
    fun requireContext(): Context

    fun openProposal(link: String)
    val session: GdkSession
    val wallet: GreenWallet
    val appFragment: AppFragment<*>

    fun overviewSetup(){
        appFragment.getNavigationResult<ScanResult>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            appFragment.viewLifecycleOwner
        ) {
            it?.let { result ->
                appFragment.clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                handleUserInput(result.result, true)
            }
        }
    }

    fun handleUserInput(data: String, isQr: Boolean = false) {
        appFragment.lifecycleScope.launch {

            val checkedInput = session.parseInput(data)

            if (checkedInput != null) {

                when (val inputType = checkedInput.second) {
                    is InputType.LnUrlAuth -> {
                        if (session.hasLightning) {
                            appFragment.navigate(
                                NavGraphDirections.actionGlobalLnUrlAuthFragment(
                                    wallet = wallet,
                                    lnUrlAuthRequest = inputType.data.toSerializable(),
                                )
                            )
                        }
                    }

                    is InputType.LnUrlWithdraw -> {
                        if (session.hasLightning) {
                            appFragment.navigate(
                                NavGraphDirections.actionGlobalLnUrlWithdrawFragment(
                                    wallet = wallet,
                                    lnUrlWithdrawRequest = inputType.data.toSerializable(),
                                )
                            )
                        }
                    }

                    else -> {
                        session.activeAccount.value?.also { activeAccount ->
                            var account = activeAccount

                            // Different network
                            if(!account.network.isSameNetwork(checkedInput.first)){
                                session.allAccounts.value.find { it.network.isSameNetwork(checkedInput.first) }?.also {
                                    account = it
                                }
                            }

                            appFragment.navigate(
                                NavGraphDirections.actionGlobalSendFragment(
                                    wallet = wallet,
                                    accountAsset = account.accountAsset,
                                    address = data,
                                    addressType = if(isQr) AddressInputType.SCAN else AddressInputType.BIP21
                                )
                            )
                        }
                    }
                }

            } else if (data.startsWith("https://")) {
                openProposal(data)
            } else {
                if (isQr) {
                    appFragment.snackbar(R.string.id_could_not_recognized_qr_code)
                } else {
                    appFragment.snackbar(R.string.id_could_not_recognized_the_uri)
                }
            }
        }
    }
}
