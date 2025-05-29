package com.blockstream.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import com.blockstream.common.events.Events
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDuoCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInUltraWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRect
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.darwin.dispatch_queue_create

@Composable
actual fun CameraView(
    modifier: Modifier,
    isFlashOn: Boolean,
    isDecodeContinuous: Boolean,
    showScanFromImage: Boolean,
    viewModel: AbstractScannerViewModel
) {
    val state = rememberPeekabooCameraState(onQrCode = {
        viewModel.postEvent(Events.SetBarcodeScannerResult(it))
    })

    Box(modifier = Modifier.fillMaxSize()) {
        PeekabooCamera(
            state = state,
            modifier = Modifier.fillMaxSize(),
            permissionDeniedContent = {
                // Custom UI content for permission denied scenario
                Text("No permissions given")
            },
        )
    }
}

// Based on https://github.com/onseok/peekaboo

internal sealed interface CameraAccess {
    data object Undefined : CameraAccess

    data object Denied : CameraAccess

    data object Authorized : CameraAccess
}

private val deviceTypes =
    listOf(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInDualWideCamera,
        AVCaptureDeviceTypeBuiltInDualCamera,
        AVCaptureDeviceTypeBuiltInUltraWideCamera,
        AVCaptureDeviceTypeBuiltInDuoCamera,
    )

@Stable
class PeekabooCameraState(
    internal var onQrCode: ((code: String) -> Unit),
) {
    var isCameraReady: Boolean by mutableStateOf(false)

    fun onCameraReady() {
        isCameraReady = true
    }
}

@Composable
fun rememberPeekabooCameraState(
    onQrCode: ((String) -> Unit) = {},
): PeekabooCameraState {
    return remember { PeekabooCameraState(onQrCode) }.apply {
        this.onQrCode = onQrCode
    }
}

@Composable
fun PeekabooCamera(
    state: PeekabooCameraState,
    modifier: Modifier,
    permissionDeniedContent: @Composable () -> Unit = {},
) {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                cameraAccess = CameraAccess.Authorized
            }

            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                cameraAccess = CameraAccess.Denied
            }

            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(
                    mediaType = AVMediaTypeVideo,
                ) { success ->
                    cameraAccess = if (success) CameraAccess.Authorized else CameraAccess.Denied
                }
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (cameraAccess) {
            CameraAccess.Undefined -> {
                // Waiting for the user to accept permission
            }

            CameraAccess.Denied -> {
                Box(modifier = modifier) {
                    permissionDeniedContent()
                }
            }

            CameraAccess.Authorized -> {
                AuthorizedCamera(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun AuthorizedCamera(
    state: PeekabooCameraState,
    modifier: Modifier = Modifier,
) {
    val camera: AVCaptureDevice? =
        remember {
            discoverySessionWithDeviceTypes(
                deviceTypes = deviceTypes,
                mediaType = AVMediaTypeVideo,
                position = AVCaptureDevicePositionBack
            ).devices.firstOrNull() as? AVCaptureDevice
        }

    if (camera != null) {
        RealDeviceCamera(
            state = state,
            camera = camera,
            modifier = modifier,
        )
    } else {
        Text(
            "Camera is not available on simulator. Please try to run on a real iOS device.",
            color = Color.White,
        )
    }

    if (!state.isCameraReady) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun RealDeviceCamera(
    state: PeekabooCameraState,
    camera: AVCaptureDevice,
    modifier: Modifier,
) {
    val queue = remember { dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0UL) }
    val metadataOutput = remember { AVCaptureMetadataOutput() }
    val metadataDelegate = remember { CameraMetadataDelegate(state.onQrCode) }

    val captureSession: AVCaptureSession = remember {
        AVCaptureSession().also { captureSession ->
            captureSession.sessionPreset = AVCaptureSessionPresetPhoto
            val captureDeviceInput: AVCaptureDeviceInput = deviceInputWithDevice(device = camera, error = null)!!
            captureSession.addInput(captureDeviceInput)

            if (captureSession.canAddOutput(metadataOutput)) {
                captureSession.addOutput(metadataOutput)

                val metadataQueue = dispatch_queue_create("metadataQueue", attr = null)

                metadataOutput.setMetadataObjectsDelegate(metadataDelegate, metadataQueue)
                metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
            }
        }
    }

    val cameraPreviewLayer = remember { AVCaptureVideoPreviewLayer(session = captureSession) }

    // Update captureSession with new camera configuration whenever isFrontCamera changed.
    LaunchedEffect(Unit) {
        val dispatchGroup = dispatch_group_create()
        captureSession.beginConfiguration()
        captureSession.inputs.forEach { captureSession.removeInput(it as AVCaptureInput) }

        val newCamera =
            discoverySessionWithDeviceTypes(
                deviceTypes,
                AVMediaTypeVideo,
                AVCaptureDevicePositionBack,
            ).devices.firstOrNull() as? AVCaptureDevice

        newCamera?.let {
            val newInput =
                AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) as AVCaptureDeviceInput
            if (captureSession.canAddInput(newInput)) {
                captureSession.addInput(newInput)
            }
        }

        captureSession.commitConfiguration()

        dispatch_group_enter(dispatchGroup)
        dispatch_async(queue) {
            captureSession.startRunning()
            dispatch_group_leave(dispatchGroup)
        }

        dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
            state.onCameraReady()
        }
    }

    DisposableEffect(cameraPreviewLayer, metadataOutput, state) {
        val listener = OrientationListener(cameraPreviewLayer)
        val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification
        NSNotificationCenter.defaultCenter.addObserver(
            observer = listener,
            selector =
                NSSelectorFromString(
                    OrientationListener::orientationDidChange.name + ":",
                ),
            name = notificationName,
            `object` = null,
        )
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(
                observer = listener,
                name = notificationName,
                `object` = null,
            )
        }
    }

    UIKitView(
        modifier = modifier,
        background = Color.Black,
        factory = {
            val dispatchGroup = dispatch_group_create()
            val cameraContainer = UIView()
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            dispatch_group_enter(dispatchGroup)
            dispatch_async(queue) {
                captureSession.startRunning()
                dispatch_group_leave(dispatchGroup)
            }
            dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                state.onCameraReady()
            }
            cameraContainer
        },
        onResize = { view: UIView, rect: CValue<CGRect> ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.setFrame(rect)
            cameraPreviewLayer.setFrame(rect)
            CATransaction.commit()
        },
    )
}

class OrientationListener(
    private val cameraPreviewLayer: AVCaptureVideoPreviewLayer,
) : NSObject() {
    @OptIn(BetaInteropApi::class)
    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        val cameraConnection = cameraPreviewLayer.connection
        val actualOrientation =
            when (UIDevice.currentDevice.orientation) {
                UIDeviceOrientation.UIDeviceOrientationPortrait ->
                    AVCaptureVideoOrientationPortrait

                UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                    AVCaptureVideoOrientationLandscapeRight

                UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                    AVCaptureVideoOrientationLandscapeLeft

                UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                    AVCaptureVideoOrientationPortrait

                else -> cameraConnection?.videoOrientation ?: AVCaptureVideoOrientationPortrait
            }
        if (cameraConnection != null) {
            cameraConnection.videoOrientation = actualOrientation
        }
    }
}

class CameraMetadataDelegate(
    private val onQrCode: ((String) -> Unit),
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        didOutputMetadataObjects.firstOrNull { it is AVMetadataMachineReadableCodeObject && it.type == AVMetadataObjectTypeQRCode }
            ?.let { it as? AVMetadataMachineReadableCodeObject }?.stringValue?.also {
                onQrCode.invoke(it)
            }
    }
}