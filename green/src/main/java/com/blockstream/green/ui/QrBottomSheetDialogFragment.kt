package com.blockstream.green.ui

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockstream.green.databinding.QrBottomSheetBinding
import com.blockstream.green.utils.createQrBitmap
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class QrBottomSheetDialogFragment: BottomSheetDialogFragment(){

    private lateinit var binding: QrBottomSheetBinding

    companion object : KLogging() {
        private const val TITLE = "TITLE"
        private const val SUBTITLE = "SUBTITLE"
        private const val CONTENT = "CONTENT"

        // Open a single instance of CameraBottomSheetDialogFragment
        fun open(fragment: AppFragment<*>, title: String, subtitle: String?, content: String){

            QrBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(TITLE, title)
                    bundle.putString(SUBTITLE, subtitle)
                    bundle.putString(CONTENT, content)
                }
                it.show(fragment.childFragmentManager, it.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = QrBottomSheetBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

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

        return binding.root
    }
}