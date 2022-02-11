package com.blockstream.green.ui.bottomsheets


import android.app.Dialog
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.blockstream.green.R
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


abstract class WalletBottomSheetDialogFragment<T : ViewDataBinding, VM : AbstractWalletViewModel>() : AbstractBottomSheetDialogFragment<T>() {

    override val segmentation : HashMap<String, Any>? by lazy { countly.subAccountSegmentation(viewModel.session, viewModel.getSubAccountLiveData().value) }

    @Suppress("UNCHECKED_CAST")
    internal val viewModel : VM by lazy {
        (requireParentFragment() as WalletFragment<*>).getWalletViewModel() as VM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Green_BottomSheetDialogTheme_Wallet)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }
}