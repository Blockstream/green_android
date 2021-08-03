package com.blockstream.green.ui.twofactor


import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.blockstream.gdk.TwoFactorResolver
import com.blockstream.gdk.data.TwoFactorStatus
import com.blockstream.green.R
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.TwofactorCodeDialogBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.localized2faMethod
import com.blockstream.green.utils.localized2faMethods
import com.blockstream.green.utils.openBrowser
import com.blockstream.green.views.GreenPinViewListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
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

    override fun selectMethod(availableMethods: List<String>): Single<String> =
        Single.create<String> { emitter ->

            // Method is already selected in the constructor
            if (selectedMethod != null) {
                emitter.onSuccess(selectedMethod)
                return@create
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

    override fun getCode(twoFactorStatus: TwoFactorStatus): Single<String> =
        Single.create<String> { emitter ->
            val dialogBinding = TwofactorCodeDialogBinding.inflate(LayoutInflater.from(context))

            dialogBinding.icon.setImageResource(TwoFactorMethod.from(twoFactorStatus.method).getIcon())
            dialogBinding.title = context.getString(R.string.id_please_provide_your_1s_code, context.localized2faMethod(twoFactorStatus.method))
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

            appFragment?.let {
                try {

                    twoFactorStatus.authData?.jsonObject?.get("telegram_url")?.jsonPrimitive?.content?.let { url ->
                        println(url)
                        it.openBrowser(url)
                    }
                } catch (e: Exception) {
                    // e.printStackTrace()
                }
            }

        }.subscribeOn(AndroidSchedulers.mainThread())
}