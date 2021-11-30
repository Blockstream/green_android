package com.blockstream.green.ui

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.blockstream.green.R
import com.blockstream.green.databinding.CameraBottomSheetBinding
import com.blockstream.green.utils.setNavigationResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class CameraBottomSheetDialogFragment: BottomSheetDialogFragment(){

    private lateinit var binding: CameraBottomSheetBinding

    private lateinit var capture: CaptureManager
    private var isTorchOn: Boolean = false

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            setNavigationResult(result = result.text, key = CAMERA_SCAN_RESULT, destinationId = findNavController().currentDestination?.id)
            dismiss()
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    companion object : KLogging() {
        const val CAMERA_SCAN_RESULT = "CAMERA_SCAN_RESULT"

        private const val DEFAULT_FRAME_THICKNESS_DP = 3f
        private const val DEFAULT_MASK_COLOR = 0x22000000
        private const val DEFAULT_FRAME_COLOR = Color.WHITE
        private const val DEFAULT_FRAME_CORNER_SIZE_DP = 50f
        private const val DEFAULT_FRAME_SIZE = 0.65f

        // Open a single instance of CameraBottomSheetDialogFragment
        fun open(fragment: AppFragment<*>){
            val cameraFragmentTag = this.javaClass.simpleName
            if(fragment.childFragmentManager.findFragmentByTag(cameraFragmentTag) == null) {
                CameraBottomSheetDialogFragment().also {
                    it.show(fragment.childFragmentManager, cameraFragmentTag)
                }
            }else{
                logger.info { "Thre is already an open istance of ${this.javaClass.simpleName}" }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CameraBottomSheetBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.viewFinder.maskColor = DEFAULT_MASK_COLOR
        binding.viewFinder.frameColor = DEFAULT_FRAME_COLOR
        binding.viewFinder.frameThickness = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_FRAME_THICKNESS_DP,
            resources.displayMetrics
        ).toInt()
        binding.viewFinder.frameCornersSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_FRAME_CORNER_SIZE_DP,
            resources.displayMetrics
        ).toInt()
        binding.viewFinder.frameSize = DEFAULT_FRAME_SIZE


        binding.decoratedBarcode.viewFinder.isVisible = false
        binding.decoratedBarcode.statusView.isVisible = false
        binding.decoratedBarcode.decodeSingle(callback)

        binding.flash.isVisible = hasFlash()
        binding.flash.setOnClickListener {
            setTorch(!isTorchOn)
        }

        capture = CaptureManager(activity, binding.decoratedBarcode)
        capture.setShowMissingCameraPermissionDialog(true)


        return binding.root
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
        setTorch(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    private fun hasFlash(): Boolean {
        return context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            ?: false
    }

    private fun setTorch(state: Boolean) {
        if (state) {
            binding.decoratedBarcode.setTorchOn()
        } else {
            binding.decoratedBarcode.setTorchOff()
        }

        isTorchOn = state
        binding.flash.setImageResource(if (state) R.drawable.ic_baseline_flash_on_24 else R.drawable.ic_baseline_flash_off_24)
    }
}