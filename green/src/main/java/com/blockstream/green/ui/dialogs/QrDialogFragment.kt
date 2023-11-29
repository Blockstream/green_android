package com.blockstream.green.ui.dialogs

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import com.blockstream.green.databinding.QrDialogBinding
import mu.KLogging

class QrDialogFragment : AbstractDialogFragment<QrDialogBinding>() {

    override fun inflate(layoutInflater: LayoutInflater): QrDialogBinding =
        QrDialogBinding.inflate(layoutInflater)

    override val screenName: String? = null

    override val isFullWidth: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BundleCompat.getParcelable(requireArguments(), QR, Bitmap::class.java)?.also {
            binding.qr.setImageDrawable(BitmapDrawable(resources, it).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        } ?: run {
            dismiss()
        }

        binding.qr.setOnClickListener {
            dismiss()
        }
    }

    companion object : KLogging() {
        private const val QR = "QR"
        fun show(bitmap: Bitmap, fragmentManager: FragmentManager) {
            showSingle(QrDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(QR, bitmap)
                }
            }, fragmentManager)
        }
    }
}
