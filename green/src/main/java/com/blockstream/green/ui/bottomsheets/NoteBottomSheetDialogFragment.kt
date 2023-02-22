package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.NoteBottomSheetBinding
import com.blockstream.green.extensions.openKeyboard
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

interface INote{
    fun saveNote(note: String)
}

@AndroidEntryPoint
class NoteBottomSheetDialogFragment : WalletBottomSheetDialogFragment<NoteBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "TransactionNote"

    override fun inflate(layoutInflater: LayoutInflater) = NoteBottomSheetBinding.inflate(layoutInflater)

    override val isAdjustResize: Boolean = true

    val note: String
        get() = requireArguments().getString(NOTE, "")

    val isLightning: Boolean
        get() = requireArguments().getBoolean(IS_LIGHTNING, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.note = note
        binding.isLightning = isLightning

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonSave.setOnClickListener {
            (viewModel as INote).saveNote(binding.note ?: "")
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
        private const val IS_LIGHTNING = "IS_LIGHTNING"

        fun show(note: String, isLightning: Boolean = false, fragmentManager: FragmentManager) {
            show(NoteBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(NOTE, note)
                    bundle.putBoolean(IS_LIGHTNING, isLightning)
                }
            }, fragmentManager)
        }
    }
}