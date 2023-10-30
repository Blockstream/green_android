package com.blockstream.green.ui.overview

import android.content.Context
import androidx.lifecycle.lifecycleScope
import breez_sdk.InputType
import com.blockstream.common.AddressInputType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.extensions.clearNavigationResult
import com.blockstream.green.extensions.getNavigationResult
import com.blockstream.green.extensions.snackbar
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.ui.bottomsheets.CameraBottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface OverviewInterface {
    fun requireContext(): Context

    fun openProposal(link: String)
    val session: GdkSession
    val wallet: GreenWallet
    val appFragment: AppFragment<*>

    fun overviewSetup(){
        appFragment.getNavigationResult<String>(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)?.observe(
            appFragment.viewLifecycleOwner
        ) {
            it?.let { result ->
                appFragment.clearNavigationResult(CameraBottomSheetDialogFragment.CAMERA_SCAN_RESULT)
                handleUserInput(result, true)
            }
        }
    }

    fun handleUserInput(data: String, isQr: Boolean = false) {
        appFragment.lifecycleScope.launch {

            val checkedInput = withContext(context = Dispatchers.IO) {
                session.parseInput(data)
            }

            if (checkedInput != null) {

                when (checkedInput.second) {
                    is InputType.LnUrlAuth -> {
                        if (session.hasLightning) {
                            appFragment.navigate(
                                NavGraphDirections.actionGlobalLnUrlAuthFragment(
                                    wallet = wallet,
                                    accountAsset = AccountAsset.fromAccount(session.lightningAccount),
                                    auth = data
                                )
                            )
                        }
                    }

                    is InputType.LnUrlWithdraw -> {
                        if (session.hasLightning) {
                            appFragment.navigate(
                                NavGraphDirections.actionGlobalLnUrlWithdrawFragment(
                                    wallet = wallet,
                                    accountAsset = AccountAsset.fromAccount(session.lightningAccount),
                                    withdraw = data
                                )
                            )
                        }
                    }

                    else -> {
                        session.activeAccount.value?.also { activeAccount ->
                            var account = activeAccount

                            // Different network
                            if(account.network.isBitcoin != checkedInput.first.isBitcoin){
                                session.allAccounts.value.find { it.isBitcoin == checkedInput.first.isBitcoin }?.also {
                                    account = it
                                }
                            }

                            appFragment.navigate(
                                NavGraphDirections.actionGlobalSendFragment(
                                    wallet = wallet,
                                    accountAsset = AccountAsset.fromAccount(account),
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
