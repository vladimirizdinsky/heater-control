package io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Device as returned by `GET /api/v1/devices` (heater-hub `docs/api.md`, section 4.1).
 *
 * Ids and enum values stay plain strings here; `DeviceMapper` turns the known ones into
 * domain enums and drops the rest (section 8).
 */
@Serializable
data class DeviceDto(
    val id: String,
    val type: String,
    val model: String,
    val friendlyName: String? = null,
    /** Relative to the server's base URL. */
    val image: String? = null,
    val online: Boolean,
    val updatedAt: String? = null,
    val attributes: List<AttributeDto> = emptyList(),
    val functions: List<FunctionDto> = emptyList(),
    /** `[]` = no faults, `null` = not known yet. */
    val faults: List<FaultDto>? = null,
    val pendingCommand: CommandDto? = null,
)

/** Body of `PUT /api/v1/devices/{id}/preferences` (section 5.1). */
@Serializable
data class PreferencesRequest(
    /** No default, so `null` (= use the model name) is always sent. */
    val friendlyName: String?,
)

/** Section 4.2. */
@Serializable
data class AttributeDto(
    val id: String,
    val type: String,
    val unit: String? = null,
    /** Boolean, number or string depending on [type]; `null` = unknown or off. */
    val value: JsonElement? = null,
    val options: List<String>? = null,
)

/** Section 4.3. */
@Serializable
data class FunctionDto(
    val id: String,
    val attribute: String? = null,
    /** Absent for functions without input. */
    val input: InputDto? = null,
    val available: Boolean,
    val unavailableReason: String? = null,
)

@Serializable
data class InputDto(
    val type: String,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val options: List<String>? = null,
    val nullable: Boolean = false,
)

/** Section 7.5. */
@Serializable
data class FaultDto(
    val code: String,
    val detail: String? = null,
)
