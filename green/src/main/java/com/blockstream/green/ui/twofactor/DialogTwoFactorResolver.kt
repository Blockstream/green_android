package com.blockstream.green.ui.twofactor


import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.blockstream.gdk.data.TwoFactorStatus
import com.blockstream.green.R
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.TwofactorCodeDialogBinding
import com.blockstream.green.extensions.localized2faMethod
import com.blockstream.green.extensions.localized2faMethods
import com.blockstream.green.gdk.TwoFactorResolver
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.GreenPinViewListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DialogTwoFactorResolver : TwoFactorResolver {
    private val context: Context
    private var appFragment: AppFragment<*>? = null
    private var selectedMethod: String?

    constructor (context: Context, selectedMethod: String? = null) {
        this.context = context
        this.selectedMethod = selectedMethod
    }

    constructor (appFragment: AppFragment<*>, selectedMethod: String? = null) {
        this.context = appFragment.requireContext()
        this.appFragment = appFragment
        this.selectedMethod = selectedMethod
    }

    override suspend fun selectMethod(availableMethods: List<String>): CompletableDeferred<String> {
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

    override suspend fun getCode(twoFactorStatus: TwoFactorStatus): CompletableDeferred<String> {
        return withContext(context = Dispatchers.Main) {
            CompletableDeferred<String>().also { deferred ->
                val dialogBinding = TwofactorCodeDialogBinding.inflate(LayoutInflater.from(context))

                twoFactorStatus.method?.let { method ->
                    dialogBinding.icon.setImageResource(TwoFactorMethod.from(method).getIcon())
                    dialogBinding.title = context.getString(
                        R.string.id_please_provide_your_1s_code,
                        context.localized2faMethod(method)
                    )
                }
                dialogBinding.hint = context.getString(R.string.id_code)

                twoFactorStatus.attemptsRemaining?.let {
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

                appFragment?.let {
                    try {
                        twoFactorStatus.authData?.jsonObject?.get("telegram_url")?.jsonPrimitive?.content?.let { url ->
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