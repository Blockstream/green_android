package com.blockstream.green.extensions

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ShareCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.gdk.isConnectionError
import com.blockstream.green.gdk.isNotAuthorized
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.utils.AppReviewHelper
import com.blockstream.green.utils.clearClipboard
import com.blockstream.green.utils.isDevelopmentFlavor
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay


fun Fragment.hideKeyboard() {
    view?.let { context?.hideKeyboard(it) }
}

fun Fragment.openKeyboard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view?.windowInsetsController?.show(WindowInsetsCompat.Type.ime())
    }else{
        (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)?.toggleSoftInputFromWindow(
            view?.applicationWindowToken, InputMethodManager.SHOW_FORCED, 0
        )
    }
}

fun Fragment.copyToClipboard(label: String, content: String, animateView: View? = null, showCopyNotification: Boolean = false) {
    com.blockstream.green.utils.copyToClipboard(
        label = label,
        content = content,
        context = requireContext(),
        animateView = animateView
    )
    if(showCopyNotification) {
        snackbar(R.string.id_copied_to_clipboard)
    }
}

fun Fragment.clearClipboard() {
    clearClipboard(requireContext())
}

fun BottomSheetDialogFragment.dismissIn(timeMillis: Long){
    lifecycleScope.launchWhenResumed {
        delay(timeMillis)
        dismiss()
    }
}

fun Context.hideKeyboard(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.windowInsetsController?.hide(WindowInsetsCompat.Type.ime())
    }else {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun Context.localized2faMethod(method: String): String = resources.getStringArray(R.array.twoFactorChoices)[resources.getStringArray(R.array.twoFactorMethods).indexOf(method)]

fun Context.localized2faMethods(methods: List<String>): List<String> = methods.map {
    localized2faMethod(it)
}

fun Context.stringFromIdentifier(id: String, vararg formatArgs: String): String? {
    if(id.startsWith("id_")) {
        val intRes = resources.getIdentifier(id, "string", BuildConfig.APPLICATION_ID)
        if (intRes > 0) {
            return getString(intRes, *formatArgs)
        }
    }
    return null
}

fun Fragment.errorFromResourcesAndGDK(throwable: Throwable): String = requireContext().errorFromResourcesAndGDK(throwable)

fun Context.errorFromResourcesAndGDK(throwable: Throwable): String = errorFromResourcesAndGDK(throwable.cause?.message ?: throwable.message ?: "An error occurred")

fun Context.errorFromResourcesAndGDK(error: String, vararg formatArgs: String): String {
    stringFromIdentifier(error, *formatArgs)?.let {
        return it
    }

    if (error.isConnectionError()) {
        return getString(R.string.id_connection_failed)
    } else if (error.isNotAuthorized()) {
        return getString(R.string.id_login_failed)
    } else if (error.contains("Breez SDK error")) {
        return if (error.contains("Self-payments")) {
            getString(R.string.id_payments_to_self_are_not)
        } else {
            val message = try {
                error.substring(error.indexOf("message: "))
            } catch (e: Exception) {
                error.replace("Breez SDK error:", "")
            }
            getString(R.string.id_an_unidentified_error_occurred, message)
        }
    }

    return error
}
fun Fragment.errorDialog(throwable: Throwable, listener: (() -> Unit)? = null) {
    errorDialog(throwable, false, listener)
}

fun Fragment.errorDialog(throwable: Throwable, showReport: Boolean, listener: (() -> Unit)? = null) {
    if (isDevelopmentFlavor) {
        throwable.printStackTrace()
    }

    // Prevent showing user triggered cancel events as errors
    if (throwable.message == "id_action_canceled") {
        listener?.invoke()
        return
    }

    errorDialog(
        error = errorFromResourcesAndGDK(throwable),
        showReport = showReport,
        listener = listener
    )
}
fun Fragment.errorDialog(error: String, listener: (() -> Unit)? = null) {
    errorDialog(error, false, listener)
}

fun Fragment.errorDialog(error: String, showReport: Boolean = false, listener: (() -> Unit)? = null) {
    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Green_MaterialAlertDialog)
        .setTitle(R.string.id_error)
        .setMessage(error)
        .setPositiveButton(android.R.string.ok, null)
        .also {
            if(showReport) {
                it.setNeutralButton(R.string.id_report) { _, _ ->
                    (this as? AppFragment<*>)?.also{
                        AppReviewHelper.showErrorReport(error, it)
                    }
                }
            }
        }
        .setOnDismissListener {
            listener?.invoke()
        }
        .show()
}

fun Fragment.dialog(title: Int, message: Int, icon: Int? = null, isMessageSelectable: Boolean = false, listener: (() -> Unit)? = null) {
    requireContext().dialog(title, message, icon, isMessageSelectable, listener)
}

fun Context.dialog(title: Int, message: Int, icon: Int? = null, isMessageSelectable: Boolean = false, listener: (() -> Unit)? = null) {
    dialog(getString(title), getString(message), icon, isMessageSelectable, listener)
}

fun Fragment.dialog(title: String, message: String, icon: Int? = null, isMessageSelectable: Boolean = false, listener: (() -> Unit)? = null): AlertDialog {
    return requireContext().dialog(title, message, icon, isMessageSelectable, listener)
}

fun Context.dialog(title: String, message: String, icon: Int? = null, isMessageSelectable: Boolean? = null, listener: (() -> Unit)? = null): AlertDialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .setOnDismissListener {
            listener?.invoke()
        }
        .apply {
            icon?.also {
                this.setIcon(it)
            }
        }
        .show().also {
            if(isMessageSelectable == true){
                it.window?.decorView?.findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
            }
        }
}

fun Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), getString(resId), duration).show()
}

fun Fragment.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), text, duration).show()
}

fun Fragment.errorSnackbar(
    throwable: Throwable,
    duration: Int = Snackbar.LENGTH_SHORT,
    print: Boolean = false
) {
    if (print) {
        throwable.printStackTrace()
    }
    snackbar(errorFromResourcesAndGDK(throwable), duration)
}

fun Fragment.snackbar(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
    snackbar(getString(resId), duration)
}

fun Fragment.snackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
    view?.let {
        Snackbar.make(it, text, duration).show()
    }
}

fun Fragment.errorSnackbar(throwable: Throwable, duration: Int = Snackbar.LENGTH_SHORT) {
    view?.let {
        Snackbar.make(it, errorFromResourcesAndGDK(throwable), duration).show()
    }
}

fun View.snackbar(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, resId, duration).show()
}

fun Fragment.share(text: String) {
    val builder = ShareCompat.IntentBuilder(requireActivity())
        .setType("text/plain")
        .setText(text)

    requireActivity().startActivity(
        Intent.createChooser(
            builder.intent,
            getString(R.string.id_share)
        )
    )
}

fun Fragment.shareJPEG(uri: Uri) {
    val builder = ShareCompat.IntentBuilder(requireActivity())
        .setType("image/jpg")
        .setStream(uri)

    requireActivity().startActivity(
        Intent.createChooser(
            builder.intent,
            getString(R.string.id_share)
        )
    )
}

fun Fragment.showPopupMenu(
    view: View,
    @MenuRes menuRes: Int,
    listener: PopupMenu.OnMenuItemClickListener
) {
    val popup = PopupMenu(requireContext(), view, Gravity.END)
    popup.menuInflater.inflate(menuRes, popup.menu)
    popup.setOnMenuItemClickListener(listener)
    popup.show()
}

fun Fragment.showChoiceDialog(
    title: String,
    items: Array<CharSequence>,
    checkedItem: Int,
    listener: (position: Int) -> Unit
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setSingleChoiceItems(items, checkedItem) { dialog: DialogInterface, position: Int ->
            listener.invoke(position)
            dialog.dismiss()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}


fun BottomSheetDialogFragment.makeItConstant(percent: Double = 1.0) {
    // Keep the height of the window always constant
    view?.also {
        val params = it.layoutParams
        params.height = (resources.displayMetrics.heightPixels * percent).toInt()
        it.layoutParams = params
    }
}
