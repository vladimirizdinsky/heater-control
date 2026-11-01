package io.github.vladimirizdinsky.heatercontrol.feature.devices.data

import io.github.vladimirizdinsky.heatercontrol.core.network.apiEnumOf
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.AttributeDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.CommandDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.DeviceDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.FaultDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.FunctionDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.InputDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.AttributeId
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.AttributeValue
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.Command
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.Device
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.DeviceFunction
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.Fault
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.FunctionId
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.Input
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.ModeValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.format.DateTimeParseException

// DTO → domain (heater-hub `docs/api.md`). Follows the compatibility rules of section 8:
// unknown attributes, functions, options and input types are dropped, unknown values become
// null, faults are always kept.

private const val TYPE_BOOL = "bool"
private const val TYPE_NUMBER = "number"
private const val TYPE_ENUM = "enum"

/** [baseUrl] resolves the relative [DeviceDto.image]. */
fun DeviceDto.toDomain(baseUrl: String) = Device(
    id = id,
    model = model,
    friendlyName = friendlyName,
    imageUrl = image?.let { baseUrl.trimEnd('/') + "/" + it.trimStart('/') },
    online = online,
    updatedAt = updatedAt?.let(::parseInstantOrNull),
    // toMap() keeps the server's order.
    attributes = attributes.mapNotNull { it.toDomain() }.toMap(),
    functions = functions.mapNotNull { it.toDomain() }.toMap(),
    faults = faults?.map { it.toDomain() },
    pendingCommandId = pendingCommand?.id,
)

fun CommandDto.toDomain() = Command(
    id = id,
    deviceId = deviceId,
    status = apiEnumOf(status),
    reason = apiEnumOf(reason),
)

private fun AttributeDto.toDomain(): Pair<AttributeId, AttributeValue>? {
    val attributeId = apiEnumOf<AttributeId>(id) ?: return null
    val primitive = value as? JsonPrimitive
    val attributeValue = when (type) {
        TYPE_BOOL -> AttributeValue.Bool(primitive?.booleanOrNull)
        TYPE_NUMBER -> AttributeValue.Number(primitive?.doubleOrNull, apiEnumOf(unit))
        TYPE_ENUM -> AttributeValue.Enum(apiEnumOf(primitive?.contentOrNull), options.toModes())
        else -> return null
    }
    return attributeId to attributeValue
}

private fun FunctionDto.toDomain(): Pair<FunctionId, DeviceFunction>? {
    val functionId = apiEnumOf<FunctionId>(id) ?: return null
    // No input is fine; an input the app does not understand drops the whole function.
    val domainInput = input?.let { it.toDomain() ?: return null }
    return functionId to DeviceFunction(
        id = functionId,
        attribute = apiEnumOf(attribute),
        input = domainInput,
        available = available,
        unavailableReason = apiEnumOf(unavailableReason),
    )
}

private fun InputDto.toDomain(): Input? = when (type) {
    TYPE_BOOL -> Input.Bool(nullable)
    TYPE_NUMBER -> Input.Number(min, max, step, nullable)
    TYPE_ENUM -> Input.Enum(options.toModes(), nullable)
    else -> null
}

private fun FaultDto.toDomain() = Fault(
    code = apiEnumOf(code),
    rawCode = code,
    detail = detail,
)

private fun List<String>?.toModes(): List<ModeValue> =
    orEmpty().mapNotNull { apiEnumOf<ModeValue>(it) }

/** A malformed timestamp must not break the whole device list. */
private fun parseInstantOrNull(text: String): Instant? = try {
    Instant.parse(text)
} catch (_: DateTimeParseException) {
    null
}
