package io.github.vladimirizdinsky.heatercontrol.feature.devices.data

import io.github.vladimirizdinsky.heatercontrol.core.network.ApiException
import io.github.vladimirizdinsky.heatercontrol.core.network.HttpClientFactory
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.ActionDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.CommandRequest
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.PreferencesRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/** Paths, methods and bodies of `docs/api.md`, sections 5.1 and 5.2. */
class DevicesApiTest {

    @Test
    fun `devices reads the device list`() = runTest {
        val engine = MockEngine { respondJson("[$DEVICE]") }

        val devices = api(engine).devices()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("$BASE_URL/api/v1/devices", request.url.toString())
        assertEquals("bf0000000000000000abcd", devices.single().id)
    }

    @Test
    fun `sendCommand posts all actions as one command`() = runTest {
        val engine = MockEngine { respondJson(COMMAND, HttpStatusCode.Accepted) }

        val command = api(engine).sendCommand(
            "bf0000000000000000abcd",
            CommandRequest(
                listOf(
                    ActionDto("setPower", JsonPrimitive(true)),
                    ActionDto("setTargetTemperature", JsonPrimitive(22)),
                    ActionDto("setSleepTimer"),
                ),
            ),
        )

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("$BASE_URL/api/v1/devices/bf0000000000000000abcd/commands", request.url.toString())
        // A missing value is left out, as the API wants for null and for functions without input.
        assertJsonBody(
            """{"actions":[{"function":"setPower","value":true},
                           {"function":"setTargetTemperature","value":22},
                           {"function":"setSleepTimer"}]}""",
            request,
        )
        assertEquals("pending", command.status)
    }

    @Test
    fun `setPreferences sends null to clear the name`() = runTest {
        val engine = MockEngine { respondJson(DEVICE) }

        api(engine).setPreferences("bf0000000000000000abcd", PreferencesRequest(friendlyName = null))

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("$BASE_URL/api/v1/devices/bf0000000000000000abcd/preferences", request.url.toString())
        assertJsonBody("""{"friendlyName":null}""", request)
    }

    @Test
    fun `command reads the command outcome`() = runTest {
        val engine = MockEngine { respondJson(COMMAND) }

        api(engine).command("4f0c")

        assertEquals("$BASE_URL/api/v1/commands/4f0c", engine.requestHistory.single().url.toString())
    }

    @Test
    fun `ids are encoded as one path segment`() = runTest {
        val engine = MockEngine { respondJson(COMMAND, HttpStatusCode.Accepted) }

        api(engine).sendCommand("a/b c", CommandRequest(listOf(ActionDto("setPower", JsonPrimitive(true)))))

        assertEquals("/api/v1/devices/a%2Fb%20c/commands", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun `a refused command throws ApiException with the error code`() = runTest {
        val engine = MockEngine {
            respondJson("""{ "error": { "code": "DEVICE_OFFLINE", "message": "offline" } }""", HttpStatusCode.Conflict)
        }

        val e = try {
            api(engine).sendCommand("bf0000000000000000abcd", CommandRequest(listOf(ActionDto("setPower", JsonPrimitive(true)))))
            null
        } catch (e: ApiException) {
            e
        }

        assertEquals(ApiException.DEVICE_OFFLINE, e?.code)
    }

    private fun api(engine: MockEngine) = DevicesApi(HttpClientFactory(engine).create(BASE_URL, token = "hh_secret"))

    private suspend fun assertJsonBody(expected: String, request: HttpRequestData) {
        assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(request.body.toByteArray().decodeToString()))
    }

    private fun MockRequestHandleScope.respondJson(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(content, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private companion object {
        const val BASE_URL = "http://192.168.0.39:8080"

        const val DEVICE = """
            { "id": "bf0000000000000000abcd", "type": "heater", "model": "Klarstein Bornholm",
              "friendlyName": null, "image": null, "online": true, "updatedAt": null,
              "attributes": [], "functions": [], "faults": [], "pendingCommand": null }
        """

        const val COMMAND = """
            { "id": "4f0c", "deviceId": "bf0000000000000000abcd", "requestedBy": "jano", "source": "api",
              "status": "pending", "actions": [ { "function": "setPower", "value": true } ],
              "attempts": 0, "createdAt": "2026-10-26T18:04:11Z", "finishedAt": null, "reason": null }
        """
    }
}
