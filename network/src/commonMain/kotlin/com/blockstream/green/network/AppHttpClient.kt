package com.blockstream.green.network

import co.touchlab.kermit.Logger
import com.blockstream.green.network.NetworkResponse.Success
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

sealed class NetworkResponse<out T> {
    data class Success<T>(val data: T) : NetworkResponse<T>()
    data class Error(val code: Int, val message: String) : NetworkResponse<Nothing>()
}

inline fun <reified T> NetworkResponse<T>.dataOrNull(): T? {
    if (this is Success) {
        return this.data
    }
    return null
}

inline fun <reified T> NetworkResponse<T>.dataOrThrow(): T {
    if (this is Success) {
        return this.data
    }
    return throw Exception((this as? NetworkResponse.Error)?.message ?: "Something went wrong")
}

abstract class AppHttpClient(
    configBlock: HttpClientConfig<*>.() -> Unit = {}
) {
    val httpClient: HttpClient = defaultHttpClient(configBlock)

    suspend inline fun <reified T> get(
        path: String, block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResponse<T> = execute {
        httpClient.get(path, block).body()
    }

    suspend inline fun <reified R : Any, reified T> get(
        resource: R, block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResponse<T> = execute {
        httpClient.get(resource = resource, block).body()
    }

    suspend inline fun <reified T, reified B> post(path: String, body: B): NetworkResponse<T> = execute {
        httpClient.post(path) {
            setBody(body)
        }.body()
    }

    suspend inline fun <reified R: Any, reified T, reified B> post(resource: R, body: B): NetworkResponse<T> = execute {
        httpClient.post(resource) {
            setBody(body)
        }.body()
    }

    suspend inline fun <reified T> execute(block: () -> HttpResponse): NetworkResponse<T> {
        return try {
            val response = block()
            if (response.status.isSuccess()) {
                return NetworkResponse.Success(response.body())
            } else {
                val message = extractError(response)
                return NetworkResponse.Error(response.status.value, message)
            }
        } catch (t: Throwable) {
            val (code, message) = extractErrorFromException(t)
            NetworkResponse.Error(code, message)
        }
    }

    suspend fun extractError(response: HttpResponse): String {
        val raw = response.bodyAsTextOrNull()

        val message = if (raw.isNullOrBlank()) {
            "Something went wrong"
        } else if (raw.trim().startsWith("<")) {
            "Something went wrong"
        } else {
            try {
                val json = Json.parseToJsonElement(raw).jsonObject
                json.values.mapNotNull { it as? JsonPrimitive }.firstNotNullOfOrNull { it.contentOrNull } ?: "Something went wrong"
            } catch (_: Exception) {
                "Something went wrong"
            }
        }

        return message
    }

    suspend fun extractErrorFromException(t: Throwable): Pair<Int, String> {
        t.printStackTrace()
        return when (t) {
            is ClientRequestException, is ServerResponseException -> {
                val response = (t as? ClientRequestException)?.response ?: (t as? ServerResponseException)?.response
                val msg = response?.bodyAsTextOrNull() ?: "Something went wrong"
                val code = response?.status?.value ?: -1
                code to msg
            }

            is SerializationException -> -2 to "Serialization error"
            is IOException -> -3 to "Network error"
            else -> -1 to (t.message ?: "Something went wrong")
        }
    }

    companion object {
        private fun defaultHttpClient(
            configBlock: HttpClientConfig<*>.() -> Unit = {}
        ): HttpClient {
            return HttpClient {
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
                configBlock()
            }
        }
    }

}


private suspend fun HttpResponse.bodyAsTextOrNull(): String? = try {
    this.bodyAsText()
} catch (_: Exception) {
    null
}