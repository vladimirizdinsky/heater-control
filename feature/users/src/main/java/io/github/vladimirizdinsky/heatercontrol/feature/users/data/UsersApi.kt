package io.github.vladimirizdinsky.heatercontrol.feature.users.data

import io.github.vladimirizdinsky.heatercontrol.core.network.ApiException
import io.github.vladimirizdinsky.heatercontrol.core.network.HttpClientFactory
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.CreatePairingRequest
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.PairRequest
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.PairResponse
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.PairingDto
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Users and pairing of heater-hub API v1 (`docs/api.md`, sections 3 and 5.0) over a client
 * from [HttpClientFactory].
 *
 * Every call throws [ApiException] for an error status and `IOException` for network failures.
 * [pair] is the only call that works with a client without a token.
 */
class UsersApi(private val client: HttpClient) {

    suspend fun pair(request: PairRequest): PairResponse =
        client.post("api/v1/pair") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun me(): UserDto = client.get("api/v1/me").body()

    /** All users, to pick whom to pair. */
    suspend fun users(): List<UserDto> = client.get("api/v1/users").body()

    suspend fun createPairing(request: CreatePairingRequest): PairingDto =
        client.post("api/v1/pairings") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
