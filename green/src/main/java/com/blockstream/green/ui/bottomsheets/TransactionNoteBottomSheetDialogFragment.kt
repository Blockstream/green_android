package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.TransactionNoteBottomSheetBinding
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.extensions.openKeyboard
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

interface ITransactionNote{
    fun saveNote(note: String)
}

@AndroidEntryPoint
class TransactionNoteBottomSheetDialogFragment : WalletBottomSheetDialogFragment<TransactionNoteBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "TransactionNote"

    override fun inflate(layoutInflater: LayoutInflater) = TransactionNoteBottomSheetBinding.inflate(layoutInflater)

    override val isAdjustResize: Boolean = true

    val note: String
        get() = requireArguments().getString(NOTE, "")


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.note = note

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            (viewModel as ITransactionNote).saveNote(binding.note ?: "")
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.textInputEditText.requestFocus()
        openKeyboard()
    }

    companion object : KLogging() {
        private const val NOTE = "NOTE"

        fun show(note: String, fragmentManager: FragmentManager) {
            show(TransactionNoteBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(NOTE, note)
                }
            }, fragmentManager)
        }
    }
}