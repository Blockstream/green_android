package com.blockstream.green.data

import co.touchlab.kermit.Logger
import com.blockstream.green.data.meld.MeldRepository
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

//expect val platformDataModule: Module

internal expect fun HttpClientFactory(
    block: HttpClientConfig<HttpClientEngineConfig>.() -> Unit = {}
): HttpClient

val dataModule = module {
    // includes(platformDataModule)

    single<HttpClient> {
        HttpClientFactory {
            install(Resources)
            install(Logging) {
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        Logger.d { message }
                    }
                }
                level = LogLevel.BODY
            }
            install(ContentNegotiation) {
                json(json = Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
        }
    }
    single {
        MeldRepository(get(), get())
    }
}