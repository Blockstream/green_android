package com.blockstream.green.ui.twofactor


import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.blockstream.gdk.TwoFactorResolver
import com.blockstream.green.R
import com.blockstream.green.databinding.TwofactorCodeDialogBinding
import com.blockstream.green.views.GreenPinViewListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single

class DialogTwoFactorResolver(val context: Context, val method: String? = null) :
    TwoFactorResolver {

    override fun selectMethod(availableMethods: List<String>): Single<String> =
        Single.create<String> { emitter ->

            // Method is already selected in the constructor
            if (method != null) {
                emitter.onSuccess(method)
                return@create
            }

            val availableMethodsChoices = availableMethods.map {
                when (it) {
                    "email" -> context.getString(R.string.id_email)
                    "sms" -> context.getString(R.string.id_sms)
                    "gauth" -> context.getString(R.string.id_authenticator_app)
                    "phone" -> context.getString(R.string.id_phone_call)
                    else -> it
                }
            }

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.id_choose_method_to_authorize_the)
                .setSingleChoiceItems(
                    availableMethodsChoices.toTypedArray(),
                    0, null
                )
                .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                    if (dialogInterface is AlertDialog) {
                        emitter.onSuccess(availableMethods[dialogInterface.listView.checkedItemPosition])
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .setOnDismissListener {
                    if (!emitter.isDisposed) {
                        emitter.tryOnError(Exception("id_action_canceled"))
                    }
                }
                .show()

        }.subscribeOn(AndroidSchedulers.mainThread())

    override fun getCode(method: String, attemptsRemaining: Int?): Single<String> =
        Single.create<String> { emitter ->

            val dialogBinding = TwofactorCodeDialogBinding.inflate(LayoutInflater.from(context))

            dialogBinding.icon.setImageResource(getIconForMethod(method))
            dialogBinding.title = context.getString(R.string.id_please_provide_your_1s_code, method)
            dialogBinding.hint = context.getString(R.string.id_code)

            attemptsRemaining?.let {
                dialogBinding.attemptsRemaining = context.getString(R.string.id_attempts_remaining_d, it)
            }

            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .setOnDismissListener {
                    if (!emitter.isDisposed) {
                        emitter.tryOnError(Exception("id_action_canceled"))
                    }
                }
                .show()

            dialogBinding.pinView.listener = object : GreenPinViewListener {
                override fun onPin(pin: String) {
                    emitter.onSuccess(pin)
                    dialog.dismiss()
                }

                override fun onPinChange(pinLength: Int, intermediatePin: String?) {}
                override fun onPinNotVerified() {}
                override fun onChangeMode(isVerify: Boolean) {}
            }

        }.subscribeOn(AndroidSchedulers.mainThread())

    private fun getIconForMethod(method: String): Int {
        return when (method) {
            "email" -> R.drawable.ic_2fa_email
            "call" -> R.drawable.ic_2fa_call
            "gauth" -> R.drawable.ic_2fa_authenticator
            else -> R.drawable.ic_2fa_sms
        }
    }
}