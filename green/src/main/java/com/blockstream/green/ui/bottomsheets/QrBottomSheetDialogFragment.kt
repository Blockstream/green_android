package com.blockstream.green.ui.bottomsheets

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.QrBottomSheetBinding
import com.blockstream.green.utils.createQrBitmap
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class QrBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<QrBottomSheetBinding>() {
    override val screenName: String = ""
    override fun inflate(layoutInflater: LayoutInflater) =
        QrBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.title = arguments?.getString(TITLE)
        binding.subtitle = arguments?.getString(SUBTITLE)

        createQrBitmap(arguments?.getString(CONTENT) ?: "")?.let {
            binding.qr.setImageDrawable(BitmapDrawable(resources, it).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        }
    }

    companion object : KLogging() {
        private const val TITLE = "TITLE"
        private const val SUBTITLE = "SUBTITLE"
        private const val CONTENT = "CONTENT"

        fun show(
            title: String,
            subtitle: String?,
            content: String,
            fragmentManager: FragmentManager
        ) {
            show(QrBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(TITLE, title)
                    bundle.putString(SUBTITLE, subtitle)
                    bundle.putString(CONTENT, content)
                }
            }, fragmentManager)
        }
    }
}