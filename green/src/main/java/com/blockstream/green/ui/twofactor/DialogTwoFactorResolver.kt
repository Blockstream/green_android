package com.blockstream.green.ui.twofactor


import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.green.databinding.TwofactorCodeDialogBinding
import com.blockstream.green.extensions.getIcon
import com.blockstream.green.extensions.localized2faMethod
import com.blockstream.green.extensions.localized2faMethods
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.utils.openNewTicket
import com.blockstream.green.views.GreenPinViewListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DialogTwoFactorResolver(
    private var appFragment: AppFragment<*>,
    private var selectedMethod: String? = null
) : TwoFactorResolver {
    private val context: Context = appFragment.requireContext()

    override suspend fun withSelectMethod(availableMethods: List<String>): CompletableDeferred<String> {
        return withContext(context = Dispatchers.Main) {
            CompletableDeferred<String>().also { deferred ->

                // Method is already selected in the constructor
                selectedMethod?.let {
                    deferred.complete(it)
                    return@also
                }

                val availableMethodsChoices = context.localized2faMethods(availableMethods)

                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.id_choose_method_to_authorize_the)
                    .setSingleChoiceItems(
                        availableMethodsChoices.toTypedArray(),
                        0, null
                    )
                    .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                        if (dialogInterface is AlertDialog) {
                            deferred.complete(availableMethods[dialogInterface.listView.checkedItemPosition])
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(false)
                    .setOnDismissListener {
                        if(!deferred.isCompleted){
                            deferred.completeExceptionally(Exception("id_action_canceled"))
                        }
                    }
                    .show()
            }
        }
    }

    override suspend fun getCode(network: Network, enable2faCallMethod: Boolean, authHandlerStatus: AuthHandlerStatus): CompletableDeferred<String> {
        return withContext(context = Dispatchers.Main) {
            CompletableDeferred<String>().also { deferred ->
                val dialogBinding = TwofactorCodeDialogBinding.inflate(LayoutInflater.from(context))

                authHandlerStatus.method?.let { method ->
                    dialogBinding.icon.setImageResource(TwoFactorMethod.from(method).getIcon())
                    dialogBinding.title = context.getString(
                        R.string.id_please_provide_your_1s_code,
                        context.localized2faMethod(method)
                    )
                }

                authHandlerStatus.attemptsRemaining?.let {
                    dialogBinding.attemptsRemaining =
                        context.getString(R.string.id_attempts_remaining_d, it)
                }

                val dialog = MaterialAlertDialogBuilder(context)
                    .setView(dialogBinding.root)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(false)
                    .setOnDismissListener {
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(Exception("id_action_canceled"))
                        }
                    }
                    .apply {
                        if (authHandlerStatus.method == "sms"){
                            setNeutralButton(R.string.id_help) { _, _ ->

                                MaterialAlertDialogBuilder(context)
                                    .setTitle(R.string.id_are_you_not_receiving_your_2fa_code)
                                    .setMessage(R.string.id_try_again_using_another_2fa_method)
                                    .apply {
                                        if(enable2faCallMethod){
                                            setPositiveButton(R.string.id_enable_2fa_call_method) { _, _ ->
                                                appFragment.getGreenViewModel()?.greenWalletOrNull?.also {
                                                    appFragment.navigate(
                                                        NavGraphDirections.actionGlobalTwoFactorSetupFragment(
                                                            wallet = it,
                                                            method = TwoFactorMethod.PHONE,
                                                            action = TwoFactorSetupAction.SETUP,
                                                            network = network,
                                                            isSmsBackup = true
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton(R.string.id_try_again, null)
                                    .setNeutralButton(R.string.id_contact_support) { _, _ ->
                                        appFragment.also {
                                            it.openNewTicket(
                                                settingsManager = it.settingsManager,
                                                subject = "Android: I am not receiving my 2FA code",
                                                errorReport = ErrorReport.createForMultisig("Android: I am not receiving my 2FA code")
                                            )
                                        }
                                    }
                                    .show()
                            }
                        }
                    }

                    .show()

                dialogBinding.pinView.listener = object : GreenPinViewListener {
                    override fun onPin(pin: String) {
                        deferred.complete(pin)
                        dialog.dismiss()
                    }

                    override fun onPinChange(pinLength: Int, intermediatePin: String?) {}
                    override fun onPinNotVerified() {}
                    override fun onChangeMode(isVerify: Boolean) {}
                }

                appFragment.let {
                    try {
                        authHandlerStatus.authData?.jsonObject?.get("telegram_url")?.jsonPrimitive?.content?.let { url ->
                            it.openBrowser(url)
                        }
                    } catch (e: Exception) {
                        // e.printStackTrace()
                    }
                }
            }
        }
    }
}