package io.github.vladimirizdinsky.heatercontrol.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HttpClientFactoryTest {

    @Test
    fun `relative paths are resolved against the base URL`() = runTest {
        val engine = MockEngine { respondJson("{}") }

        HttpClientFactory(engine).create("http://192.168.0.39:8080", token = null).get("api/v1/me")
        HttpClientFactory(engine).create("https://pi.tailnet.ts.net/hub", token = null).get("api/v1/me")

        assertEquals("http://192.168.0.39:8080/api/v1/me", engine.requestHistory[0].url.toString())
        assertEquals("https://pi.tailnet.ts.net/hub/api/v1/me", engine.requestHistory[1].url.toString())
    }

    @Test
    fun `sends the token as a bearer header`() = runTest {
        val engine = MockEngine { respondJson("{}") }

        HttpClientFactory(engine).create(BASE_URL, token = "hh_secret").get("api/v1/me")

        assertEquals("Bearer hh_secret", engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `sends no authorization header without a token`() = runTest {
        val engine = MockEngine { respondJson("{}") }

        HttpClientFactory(engine).create(BASE_URL, token = null).get("api/v1/pair")

        assertNull(engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a successful response is passed through`() = runTest {
        val engine = MockEngine { respondJson("""{"status":"ok"}""") }

        val body = HttpClientFactory(engine).create(BASE_URL, token = null).get("health").bodyAsText()

        assertEquals("""{"status":"ok"}""", body)
    }

    @Test
    fun `an error response becomes ApiException with the server's code`() = runTest {
        val engine = MockEngine {
            respondJson(
                """{ "error": { "code": "DEVICE_OFFLINE", "message": "Device is not connected" } }""",
                HttpStatusCode.Conflict,
            )
        }

        val e = apiException { HttpClientFactory(engine).create(BASE_URL, "hh_secret").get("api/v1/devices") }

        assertEquals(409, e.status)
        assertEquals(ApiException.DEVICE_OFFLINE, e.code)
        assertTrue(e.message.orEmpty().contains("Device is not connected"))
    }

    @Test
    fun `an error page of another server becomes ApiException without a code`() = runTest {
        val engine = MockEngine {
            respond(
                "<html><body>Not Found</body></html>",
                HttpStatusCode.NotFound,
                headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()),
            )
        }

        val e = apiException { HttpClientFactory(engine).create(BASE_URL, token = null).get("api/v1/pair") }

        assertEquals(404, e.status)
        assertNull(e.code)
    }

    @Test
    fun `a network failure stays an IOException`() = runTest {
        val engine = MockEngine { throw IOException("Connection refused") }

        val thrown = try {
            HttpClientFactory(engine).create(BASE_URL, token = null).get("api/v1/me")
            null
        } catch (e: IOException) {
            e
        }

        assertEquals("Connection refused", thrown?.message)
    }

    private suspend fun apiException(block: suspend () -> Unit): ApiException {
        try {
            block()
        } catch (e: ApiException) {
            return e
        }
        throw AssertionError("ApiException expected")
    }

    private companion object {
        const val BASE_URL = "http://192.168.0.39:8080"
    }
}

/** A JSON response, as heater-hub sends it. */
private fun MockRequestHandleScope.respondJson(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(content, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
