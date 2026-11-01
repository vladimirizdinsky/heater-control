package io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model

import java.time.Instant

/**
 * A device as the app sees it: only known attributes and functions, in the server's order
 * (both maps keep insertion order).
 */
data class Device(
    val id: String,
    /** Readable product name, e.g. `Klarstein Bornholm`. */
    val model: String,
    /** The user's own name, or `null`. */
    val friendlyName: String?,
    /** Already absolute, or `null` if the model has no picture. */
    val imageUrl: String?,
    val online: Boolean,
    /** Last report from the device; `null` before the first one. */
    val updatedAt: Instant?,
    val attributes: Map<AttributeId, AttributeValue>,
    val functions: Map<FunctionId, DeviceFunction>,
    /** `[]` = no faults, `null` = not known yet. */
    val faults: List<Fault>?,
    val pendingCommandId: String?,
) {
    val displayName: String get() = friendlyName ?: model
}

/** Current value of an attribute; `value == null` means unknown or, for nullable ones, off. */
sealed interface AttributeValue {
    data class Bool(val value: Boolean?) : AttributeValue
    data class Number(val value: Double?, val unit: MeasureUnit?) : AttributeValue
    /** [value] is `null` also when the device reports a mode the app does not know. */
    data class Enum(val value: ModeValue?, val options: List<ModeValue>) : AttributeValue
}

data class DeviceFunction(
    val id: FunctionId,
    /** The attribute this function sets, if any. */
    val attribute: AttributeId?,
    /** `null` for a function without input. */
    val input: Input?,
    val available: Boolean,
    /** Only when not [available]; `null` also for a reason the app does not know. */
    val unavailableReason: UnavailableReason?,
)

/** What a function takes. A function with an unknown input type is not mapped at all. */
sealed interface Input {
    /** `null` is allowed, e.g. a sleep timer that is off. */
    val nullable: Boolean

    data class Bool(override val nullable: Boolean) : Input
    data class Number(val min: Double?, val max: Double?, val step: Double?, override val nullable: Boolean) : Input
    data class Enum(val options: List<ModeValue>, override val nullable: Boolean) : Input
}

/** Faults are never hidden, so an unknown one keeps its [rawCode] and gets [code] `null`. */
data class Fault(
    val code: FaultCode?,
    val rawCode: String,
    /** E.g. the raw device code `dyson:fs03` of a [FaultCode.DEVICE_FAULT]. */
    val detail: String?,
)
