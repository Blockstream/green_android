package com.blockstream.common.models.abstract

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.BcurResolver
import com.blockstream.common.gdk.params.BcurDecodeParams
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AbstractScannerViewModel(val isDecodeContinuous: Boolean = false, greenWalletOrNull: GreenWallet? = null): GreenViewModel(greenWalletOrNull = greenWalletOrNull) {

    private var isScanComplete = false

    private var bcurPartEmitter: CompletableDeferred<String>? = null

    abstract fun setScanResult(scanResult: ScanResult)

    private fun barcodeScannerResult(scanResult: ScanResult){
        if (appInfo.isDevelopmentOrDebug) {
            logger.d { "QR (DevelopmentOrDebug): $scanResult" }
        }

        isScanComplete = true
        setScanResult(scanResult)
    }

    internal fun resetScanner(){
        isScanComplete = false
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is Events.SetBarcodeScannerResult) {
            val scannedText = event.scannedText

            if(!isScanComplete) {
                if ((isDecodeContinuous && scannedText.startsWith(prefix = "ur:", ignoreCase = true)) || bcurPartEmitter != null) {
                    if (bcurPartEmitter == null) {
                        viewModelScope.coroutineScope.launch(context = logException(countly)) {
                            try {
                                val bcurDecodedData = withContext(context = Dispatchers.IO) {
                                    session.bcurDecode(
                                        BcurDecodeParams(part = scannedText),
                                        object : BcurResolver {
                                            override fun requestData(): CompletableDeferred<String> {
                                                return CompletableDeferred<String>().also {
                                                    bcurPartEmitter = it
                                                }
                                            }
                                        }
                                    )
                                }

                                barcodeScannerResult(ScanResult.from(bcurDecodedData))
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
                    barcodeScannerResult(ScanResult(scannedText))
                }
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        try {
            bcurPartEmitter?.completeExceptionally(Exception(""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object: Loggable()
}