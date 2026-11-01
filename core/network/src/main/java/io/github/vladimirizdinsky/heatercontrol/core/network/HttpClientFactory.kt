package io.github.vladimirizdinsky.heatercontrol.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val HEADER_BEARER = "Bearer"

/**
 * Creates one [HttpClient] per server and token, e.g. without a token for pairing and with
 * one after it. All clients share [engine], so closing a client leaves the engine running.
 *
 * Requests use paths relative to the base URL without a leading slash (`api/v1/devices`).
 * Error statuses are thrown as [ApiException]; network failures stay `IOException`.
 */
class HttpClientFactory(
    private val engine: HttpClientEngine = OkHttp.create(),
) {

    fun create(baseUrl: String, token: String?): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            // LAN or Tailscale: a server that does not answer quickly is not reachable.
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
        }
        defaultRequest {
            // The trailing slash keeps a path in the base URL when relative paths are resolved.
            url(baseUrl.trimEnd('/') + "/")
            token?.let { header(HttpHeaders.Authorization, "$HEADER_BEARER $it") }
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (!response.status.isSuccess()) throw response.toApiException()
            }
        }
    }

    private suspend fun HttpResponse.toApiException(): ApiException {
        val error = try {
            json.decodeFromString<ErrorResponse>(bodyAsText()).error
        } catch (_: IllegalArgumentException) {
            null // not a heater-hub error body (SerializationException is a subclass)
        }
        return ApiException(status.value, error?.code, error?.message)
    }

    companion object {
        /** Section 8 of the API: unknown keys are ignored. Defaults are not encoded, so optional request keys are left out. */
        val json = Json { ignoreUnknownKeys = true }
    }
}
