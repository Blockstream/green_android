package com.blockstream.green.ui.bottomsheets

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.blockstream.green.R
import com.blockstream.green.databinding.CameraBottomSheetBinding
import com.blockstream.green.extensions.errorDialog
import com.blockstream.green.extensions.makeItConstant
import com.blockstream.green.extensions.setNavigationResult
import com.blockstream.green.ui.onboarding.AbstractOnboardingFragment
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.MixedDecoder
import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.URDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging


class CameraBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<CameraBottomSheetBinding>(){

    override val screenName = "Scan"

    override fun inflate(layoutInflater: LayoutInflater) = CameraBottomSheetBinding.inflate(layoutInflater)

    private lateinit var capture: CaptureManager
    private var isTorchOn: Boolean = false
    override val expanded: Boolean
        get() = true

    private val isDecodeContinuous by lazy { arguments?.getBoolean(DECODE_CONTINUOUS, false) == true }

    private val urDecoder by lazy { URDecoder() }

    private var openGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.also {
            handleImage(it)
        }
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {

            if(isDecodeContinuous){
                if(urDecoder.result == null){
                    urDecoder.receivePart(result.text)

                    if(isDevelopmentOrDebug) {
                        logger.info { result.text }
                        logger.info { "Progress: ${(100 * urDecoder.estimatedPercentComplete).toInt()}" }
                    }

                    binding.progress = (100 * urDecoder.estimatedPercentComplete).toInt()

                    // Result complete
                    if(urDecoder.result != null){
                        val urResult = urDecoder.result
                        if (urResult.type == ResultType.SUCCESS) {
                            setResultAndDismiss(urResult.ur.toString())
                        }
                    }
                }
            }else{
                setResultAndDismiss(result.text)
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Keep the height of the window always constant
        makeItConstant(0.85)

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonGallery.setOnClickListener {
            openGallery.launch("image/*")
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


        binding.decoratedBarcode.apply {
            viewFinder.isVisible = false
            statusView.isVisible = false
        }

        binding.decoratedBarcode.cameraSettings.apply {
            isMeteringEnabled = true
            isExposureEnabled = true
            isContinuousFocusEnabled = true
        }

        if(isDecodeContinuous){
            binding.decoratedBarcode.decodeContinuous(callback)
        }else{
            binding.decoratedBarcode.decodeSingle(callback)
        }

        binding.flash.isVisible = hasFlash()
        binding.flash.setOnClickListener {
            setTorch(!isTorchOn)
        }

        capture = CaptureManager(activity, binding.decoratedBarcode)
        capture.setShowMissingCameraPermissionDialog(true)
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

    private fun handleImage(uri: Uri) {
        lifecycleScope.launch(context = Dispatchers.IO) {
            try {
                val image = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                    ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.RGBA_F16, true)
                }

                val intArray = IntArray(image.width * image.height)
                image.getPixels(intArray, 0, image.width, 0, 0, image.width, image.height)

                val source = RGBLuminanceSource(image.width, image.height, intArray)
                val reader = MixedDecoder(MultiFormatReader())
                var result = reader.decode(source)
                if (result == null) {
                    result = reader.decode(source)
                }

                withContext(context = Dispatchers.Main) {
                    if (result != null) {
                        callback.barcodeResult(BarcodeResult(result, null))
                    } else {
                        errorDialog(getString(R.string.id_could_not_recognized_qr_code))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setResultAndDismiss(result: String){
        if(isDevelopmentOrDebug){
            logger.info { "QR (DevelopmentOrDebug): $result" }
        }

        val session = (requireParentFragment() as? AbstractWalletFragment<*>)?.getWalletViewModel()?.session
        val setupArgs = (requireParentFragment() as? AbstractOnboardingFragment<*>)?.setupArgs
        countly.qrScan(session = session, setupArgs = setupArgs, arguments?.getString(SCREEN_NAME))

        setNavigationResult(result = result, key = CAMERA_SCAN_RESULT, destinationId = findNavController().currentDestination?.id)
        dismiss()
    }

    companion object : KLogging() {
        private const val SCREEN_NAME = "SCREEN_NAME"
        const val DECODE_CONTINUOUS = "DECODE_CONTINUOUS"
        const val CAMERA_SCAN_RESULT = "CAMERA_SCAN_RESULT"

        private const val DEFAULT_FRAME_THICKNESS_DP = 3f
        private const val DEFAULT_MASK_COLOR = 0x22000000
        private const val DEFAULT_FRAME_COLOR = Color.WHITE
        private const val DEFAULT_FRAME_CORNER_SIZE_DP = 50f
        private const val DEFAULT_FRAME_SIZE = 0.65f

        fun showSingle(screenName: String?, decodeContinuous: Boolean = false, fragmentManager: FragmentManager){
            showSingle(CameraBottomSheetDialogFragment().also {
                it.arguments = Bundle().apply {
                    putBoolean(DECODE_CONTINUOUS, decodeContinuous)
                    putString(SCREEN_NAME, screenName)
                }
            }, fragmentManager)
        }
    }
}
