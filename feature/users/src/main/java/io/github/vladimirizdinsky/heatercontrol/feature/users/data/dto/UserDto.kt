package io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto

import kotlinx.serialization.Serializable

/** `GET /api/v1/me` and one item of `GET /api/v1/users` (heater-hub `docs/api.md`, section 5). */
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    /** `user` or `admin`. */
    val role: String,
)
