package io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model

/** The part of a command the app follows until it is no longer pending (spec 4.1). */
data class Command(
    val id: String,
    val deviceId: String,
    /** `null` for a status the app does not know; treated as finished. */
    val status: CommandStatus?,
    /** Why it failed or was aborted, otherwise `null`. */
    val reason: CommandReason?,
)
