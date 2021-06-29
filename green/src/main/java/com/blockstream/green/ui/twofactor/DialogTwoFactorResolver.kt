package com.blockstream.green.ui.twofactor


import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.blockstream.gdk.TwoFactorResolver
import com.blockstream.green.R
import com.blockstream.green.databinding.EditTextDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single

class DialogTwoFactorResolver(val context: Context, val method: String? = null) : TwoFactorResolver {

    override fun selectMethod(availableMethods: List<String>): Single<String> =
        Single.create<String> { emitter ->

            // Method is already selected in the constructor
            if(method != null){
                emitter.onSuccess(method)
                return@create
            }

            val availableMethodsChoices = availableMethods.map {
                when(it){
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
                .setPositiveButton(android.R.string.ok){ dialogInterface: DialogInterface, _: Int ->
                    if(dialogInterface is AlertDialog){
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

            val dialogBinding = EditTextDialogBinding.inflate(LayoutInflater.from(context))

            dialogBinding.hint = context.getString(R.string.id_code)
            dialogBinding.editText.inputType = InputType.TYPE_CLASS_NUMBER
            attemptsRemaining?.let{
                dialogBinding.textInputLayout.helperText = context.getString(R.string.id_attempts_remaining_d, it)
            }

           val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.id_please_provide_your_1s_code, method))
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.id_ok) { _, _ ->
                    emitter.onSuccess(dialogBinding.text ?: "")
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .setOnDismissListener {
                    if (!emitter.isDisposed) {
                        emitter.tryOnError(Exception("id_action_canceled"))
                    }
                }
                .show()

            // Auto proceed on 6 digits input
            dialogBinding.editText.doAfterTextChanged {
                if(it?.length == 6){
                    emitter.onSuccess(dialogBinding.text ?: "")
                    dialog.dismiss()
                }
            }

            // set focus to the input field
            dialogBinding.editText.requestFocus()

        }.subscribeOn(AndroidSchedulers.mainThread())

}