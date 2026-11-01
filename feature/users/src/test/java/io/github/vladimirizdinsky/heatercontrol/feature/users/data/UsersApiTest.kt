package io.github.vladimirizdinsky.heatercontrol.feature.users.data

import io.github.vladimirizdinsky.heatercontrol.core.network.ApiException
import io.github.vladimirizdinsky.heatercontrol.core.network.HttpClientFactory
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.CreatePairingRequest
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.PairRequest
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.UserDto
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Paths, methods and bodies of `docs/api.md`, sections 3 and 5.0. */
class UsersApiTest {

    @Test
    fun `pair redeems the code without a token and returns the token`() = runTest {
        val engine = MockEngine { respondJson(PAIR_RESPONSE, HttpStatusCode.Created) }

        val response = api(engine, token = null).pair(PairRequest(code = "k7qm-4xpt", clientName = "Pixel 8"))

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("$BASE_URL/api/v1/pair", request.url.toString())
        assertNull(request.headers[HttpHeaders.Authorization])
        assertJsonBody("""{"code":"k7qm-4xpt","clientName":"Pixel 8"}""", request)
        assertEquals("anna", response.user.id)
        assertEquals("hh_abc", response.token.token)
    }

    @Test
    fun `me reads the calling user`() = runTest {
        val engine = MockEngine { respondJson("""{ "id": "jano", "name": "Jano", "role": "admin" }""") }

        val me = api(engine).me()

        val request = engine.requestHistory.single()
        assertEquals("$BASE_URL/api/v1/me", request.url.toString())
        assertEquals("Bearer hh_secret", request.headers[HttpHeaders.Authorization])
        assertEquals("Jano", me.name)
    }

    @Test
    fun `users reads all users`() = runTest {
        val engine = MockEngine {
            respondJson("""[ { "id": "jano", "name": "Jano", "role": "admin" }, { "id": "anna", "name": "Anna", "role": "user" } ]""")
        }

        val users = api(engine).users()

        assertEquals("$BASE_URL/api/v1/users", engine.requestHistory.single().url.toString())
        assertEquals(listOf("jano", "anna"), users.map { it.id })
    }

    @Test
    fun `createPairing for an existing user sends only userId`() = runTest {
        val engine = MockEngine { respondJson(PAIRING, HttpStatusCode.Created) }

        api(engine).createPairing(CreatePairingRequest(userId = "anna"))

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("$BASE_URL/api/v1/pairings", request.url.toString())
        assertJsonBody("""{"userId":"anna"}""", request)
    }

    @Test
    fun `createPairing for a new user sends only newUser`() = runTest {
        val engine = MockEngine { respondJson(PAIRING, HttpStatusCode.Created) }

        api(engine).createPairing(CreatePairingRequest(newUser = UserDto(id = "anna", name = "Anna", role = "user")))

        assertJsonBody("""{"newUser":{"id":"anna","name":"Anna","role":"user"}}""", engine.requestHistory.single())
    }

    @Test
    fun `an invalid pairing code throws ApiException with its code`() = runTest {
        val engine = MockEngine {
            respondJson("""{ "error": { "code": "PAIRING_CODE_INVALID", "message": "unknown code" } }""", HttpStatusCode.BadRequest)
        }

        val e = pairFailure(engine)

        assertEquals(400, e.status)
        assertEquals(ApiException.PAIRING_CODE_INVALID, e.code)
    }

    @Test
    fun `too many attempts throws ApiException with its code`() = runTest {
        val engine = MockEngine {
            respondJson("""{ "error": { "code": "TOO_MANY_ATTEMPTS", "message": "slow down" } }""", HttpStatusCode.TooManyRequests)
        }

        val e = pairFailure(engine)

        assertEquals(429, e.status)
        assertEquals(ApiException.TOO_MANY_ATTEMPTS, e.code)
    }

    private fun api(engine: MockEngine, token: String? = "hh_secret") =
        UsersApi(HttpClientFactory(engine).create(BASE_URL, token))

    private suspend fun pairFailure(engine: MockEngine): ApiException {
        try {
            api(engine, token = null).pair(PairRequest(code = "K7QM-4XPT", clientName = "Pixel 8"))
        } catch (e: ApiException) {
            return e
        }
        throw AssertionError("ApiException expected")
    }

    private suspend fun assertJsonBody(expected: String, request: HttpRequestData) {
        assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(request.body.toByteArray().decodeToString()))
    }

    private fun MockRequestHandleScope.respondJson(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(content, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private companion object {
        const val BASE_URL = "http://192.168.0.39:8080"

        const val PAIR_RESPONSE = """
            { "user": { "id": "anna", "name": "Anna", "role": "user" },
              "token": { "id": "3f9a01c2", "name": "Pixel 8", "createdAt": "2026-10-28T12:01:00Z",
                         "lastUsedAt": null, "token": "hh_abc" } }
        """

        const val PAIRING = """
            { "id": "5c01e7aa", "user": { "id": "anna", "name": "Anna", "role": "user" },
              "code": "K7QM-4XPT",
              "uri": "heaterhub://pair?server=http%3A%2F%2F192.168.0.39%3A8080&code=K7QM4XPT",
              "expiresAt": "2026-10-28T12:10:00Z" }
        """
    }
}
