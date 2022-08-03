package com.blockstream.green.ui.swap

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.SwapProposal
import com.blockstream.green.data.Countly
import com.blockstream.green.data.HerokuResponse
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.createQrBitmap
import com.blockstream.green.extensions.logException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import mu.KLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class SwapProposalViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted proposal: SwapProposal,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    var qrBitmap = MutableLiveData<Bitmap?>()
    var currentFrame = MutableLiveData<Int>()
    var totalFrames = MutableLiveData<Int>()
    var link = MutableLiveData<String?>()

    init {
        viewModelScope.launch(context = Dispatchers.IO + logException(countly)) {
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
                GdkBridge.JsonDeserializer.decodeFromString<HerokuResponse>(it)
            }.let {
                "/proposals/${it.proposalId}"
            }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            proposal: SwapProposal
        ): SwapProposalViewModel
    }

    companion object : KLogging() {

        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            proposal: SwapProposal
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, proposal) as T
            }
        }
    }
}
