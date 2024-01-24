package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.green.databinding.NoteBottomSheetBinding
import com.blockstream.green.extensions.openKeyboard
import mu.KLogging

interface INote{
    fun saveNote(note: String)
}

class NoteBottomSheetDialogFragment : WalletBottomSheetDialogFragment<NoteBottomSheetBinding, GreenViewModel>() {
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
            (viewModel as? INote)?.saveNote(binding.note ?: "")

            (viewModel as? ReceiveViewModel)?.also {
                it.postEvent(ReceiveViewModel.LocalEvents.SetNote(binding.note ?: ""))
            }
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