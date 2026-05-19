package com.blockstream.domain.receive

import com.blockstream.data.data.AppConfig
import com.blockstream.data.platformFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath

class SaveAndShareQrCodeUseCase(
    private val appConfig: AppConfig
) {
    suspend operator fun invoke(qrBytes: ByteArray?): Path? {
        if (qrBytes == null) return null

        val fileSystem = platformFileSystem()
        val cachePath = "${appConfig.cacheDir}/Green_QR_Code.jpeg".toPath()

        withContext(Dispatchers.IO) {
            fileSystem.write(cachePath) {
                this.write(qrBytes)
            }
        }

        return cachePath
    }
}