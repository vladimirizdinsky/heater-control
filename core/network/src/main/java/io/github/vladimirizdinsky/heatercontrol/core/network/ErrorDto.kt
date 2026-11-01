package io.github.vladimirizdinsky.heatercontrol.core.network

import kotlinx.serialization.Serializable

/** Body of every error response (heater-hub `docs/api.md`, section 6). */
@Serializable
data class ErrorResponse(
    val error: ErrorDto,
)

@Serializable
data class ErrorDto(
    /** E.g. `DEVICE_OFFLINE`; the client branches on it. */
    val code: String,
    /** English developer text for logs, never shown to users. */
    val message: String? = null,
)
