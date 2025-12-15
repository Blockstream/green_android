package com.blockstream.compose.models.abstract

import androidx.lifecycle.viewModelScope
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.ScanResult
import com.blockstream.data.extensions.logException
import com.blockstream.data.gdk.BcurResolver
import com.blockstream.data.gdk.params.BcurDecodeParams
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class AbstractScannerViewModel(val isDecodeContinuous: Boolean = false, greenWalletOrNull: GreenWallet? = null) :
    GreenViewModel(greenWalletOrNull = greenWalletOrNull) {

    private var isScanComplete = false

    private var bcurPartEmitter: CompletableDeferred<String>? = null

    abstract fun setScanResult(scanResult: ScanResult)

    internal val _progress = MutableStateFlow<Int?>(null)
    val progress: StateFlow<Int?> = _progress

    private val mutex = Mutex()
    private suspend fun barcodeScannerResult(scanResult: ScanResult) {
        if (appInfo.isDevelopmentOrDebug) {
            logger.d { "QR (DevelopmentOrDebug): $scanResult" }
        }

        mutex.withLock {
            if (!isScanComplete) {
                isScanComplete = true
                setScanResult(scanResult)
            }
        }
    }

    internal fun resetScanner() {
        isScanComplete = false
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.SetBarcodeScannerResult) {
            val scannedText = event.scannedText

            if (!isScanComplete) {
                if ((isDecodeContinuous && scannedText.startsWith(prefix = "ur:", ignoreCase = true)) || bcurPartEmitter != null) {
                    if (bcurPartEmitter == null) {
                        viewModelScope.launch(context = logException(countly)) {

                            try {
                                val bcurDecodedData = session.bcurDecode(
                                    params = BcurDecodeParams(part = scannedText),
                                    bcurResolver = object : BcurResolver {
                                        override fun requestData(): CompletableDeferred<String> {
                                            return CompletableDeferred<String>().also {
                                                bcurPartEmitter = it
                                            }
                                        }

                                        override fun progress(progress: Int) {
                                            _progress.value = progress
                                        }
                                    }
                                )

                                barcodeScannerResult(ScanResult.from(bcurDecodedData))
                            } catch (e: CancellationException) {
                                e.printStackTrace()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                postSideEffect(SideEffects.ErrorDialog(e))
                            } finally {
                                bcurPartEmitter = null
                            }
                        }
                    } else {
                        if (bcurPartEmitter?.isCompleted == false) {
                            bcurPartEmitter?.complete(scannedText)
                        }
                    }
                } else {
                    // launch a new coroutine to avoid blocking the main thread
                    viewModelScope.launch(context = logException(countly)) {
                        barcodeScannerResult(ScanResult(scannedText))
                    }
                }
            }
        }
    }

    // Called from Android ViewModelX
    override fun onCleared() {
        super.onCleared()
        try {
            bcurPartEmitter?.completeExceptionally(Exception(""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object : Loggable()
}