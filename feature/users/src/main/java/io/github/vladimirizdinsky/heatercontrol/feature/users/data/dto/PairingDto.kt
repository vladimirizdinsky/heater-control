package io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto

import kotlinx.serialization.Serializable

// Pairing (heater-hub `docs/api.md`, sections 3 and 5.0).

/** Body of `POST /api/v1/pairings`: exactly one of [userId] and [newUser]. */
@Serializable
data class CreatePairingRequest(
    val userId: String? = null,
    /** Admins only. */
    val newUser: UserDto? = null,
)

/** Response of `POST /api/v1/pairings`. */
@Serializable
data class PairingDto(
    val id: String,
    val user: UserDto,
    /** Shown as text, e.g. `K7QM-4XPT`. */
    val code: String,
    /** Content of the QR code: `heaterhub://pair?server=…&code=…`. */
    val uri: String,
    val expiresAt: String,
)

/** Body of `POST /api/v1/pair`; sent without a token. */
@Serializable
data class PairRequest(
    val code: String,
    /** Becomes the token's name, e.g. `Build.MODEL`. */
    val clientName: String,
)

/** Response of `POST /api/v1/pair`. */
@Serializable
data class PairResponse(
    val user: UserDto,
    val token: TokenDto,
)

@Serializable
data class TokenDto(
    val id: String,
    val name: String,
    val createdAt: String,
    val lastUsedAt: String? = null,
    /** The bearer token itself; the server sends it only in the pair response. */
    val token: String,
)
