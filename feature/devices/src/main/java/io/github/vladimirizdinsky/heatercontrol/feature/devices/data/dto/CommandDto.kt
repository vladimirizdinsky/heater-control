package io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Command (heater-hub `docs/api.md`, section 4.4); status and reason ids in section 7.7. */
@Serializable
data class CommandDto(
    val id: String,
    val deviceId: String,
    /** `null` for commands not started by a user (schedules, later). */
    val requestedBy: String? = null,
    val source: String,
    val status: String,
    val actions: List<ActionDto> = emptyList(),
    val attempts: Int = 0,
    val createdAt: String,
    val finishedAt: String? = null,
    val reason: String? = null,
)

/** Body of `POST /api/v1/devices/{id}/commands` (section 5.2). */
@Serializable
data class CommandRequest(
    val actions: List<ActionDto>,
)

/** One function call of a command. */
@Serializable
data class ActionDto(
    val function: String,
    /** Matches the function's input type; `null` for a nullable input (timer off) or no input. */
    val value: JsonElement? = null,
)
