package com.blockstream.green.ui.swap

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.HerokuResponse
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.data.SwapProposal
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.createQrBitmap
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@KoinViewModel
class SwapProposalViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam proposal: SwapProposal,
) : AbstractWalletViewModel(wallet) {

    var qrBitmap = MutableLiveData<Bitmap?>()
    var currentFrame = MutableLiveData<Int>()
    var totalFrames = MutableLiveData<Int>()
    var link = MutableLiveData<String?>()

    init {
        viewModelScope.coroutineScope.launch(context = Dispatchers.IO + logException(countly)) {
            upload(proposal).let {
                link.postValue(it)
                qrBitmap.postValue(createQrBitmap(it, errorCorrectionLevel = ErrorCorrectionLevel.H))
            }
        }
    }

    private fun upload(proposal: SwapProposal): String {
        return URL("")
            .openConnection()
            .let {
                it as HttpURLConnection
            }.apply {
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                requestMethod = "POST"
                doOutput = true
                val outputWriter = OutputStreamWriter(outputStream)
                outputWriter.write(proposal.proposal)
                outputWriter.flush()
            }.let {
                if (it.responseCode == 200) it.inputStream else it.errorStream
            }.let { streamToRead ->
                BufferedReader(InputStreamReader(streamToRead)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()
                    response.toString()
                }
            }.let {
                JsonDeserializer.decodeFromString<HerokuResponse>(it)
            }.let {
                "/proposals/${it.proposalId}"
            }
    }
}
