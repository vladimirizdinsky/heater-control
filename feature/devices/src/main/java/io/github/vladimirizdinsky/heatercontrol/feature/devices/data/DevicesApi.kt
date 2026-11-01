package io.github.vladimirizdinsky.heatercontrol.feature.devices.data

import io.github.vladimirizdinsky.heatercontrol.core.network.ApiException
import io.github.vladimirizdinsky.heatercontrol.core.network.HttpClientFactory
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.CommandDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.CommandRequest
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.DeviceDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.PreferencesRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart

/**
 * Devices and commands of heater-hub API v1 (`docs/api.md`, sections 5.1 and 5.2) over a
 * client with a token from [HttpClientFactory].
 *
 * Every call throws [ApiException] for an error status and `IOException` for network failures.
 * Ids are opaque, so they are encoded as one path segment.
 */
class DevicesApi(private val client: HttpClient) {

    suspend fun devices(): List<DeviceDto> = client.get("api/v1/devices").body()

    /** Returns the updated device. */
    suspend fun setPreferences(deviceId: String, request: PreferencesRequest): DeviceDto =
        client.put("api/v1/devices/${deviceId.encodeURLPathPart()}/preferences") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** Returns the command with status `pending` (`202 Accepted`). */
    suspend fun sendCommand(deviceId: String, request: CommandRequest): CommandDto =
        client.post("api/v1/devices/${deviceId.encodeURLPathPart()}/commands") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun command(commandId: String): CommandDto =
        client.get("api/v1/commands/${commandId.encodeURLPathPart()}").body()
}
