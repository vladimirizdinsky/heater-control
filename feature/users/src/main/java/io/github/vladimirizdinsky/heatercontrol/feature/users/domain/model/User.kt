package io.github.vladimirizdinsky.heatercontrol.feature.users.domain.model

import io.github.vladimirizdinsky.heatercontrol.core.network.ApiEnum
import java.time.Instant

data class User(
    val id: String,
    val name: String,
    /** An unknown role is mapped to [UserRole.USER], the one with fewer rights. */
    val role: UserRole,
)

/** heater-hub `docs/api.md`, section 3. */
enum class UserRole(override val apiId: String) : ApiEnum {
    USER("user"),
    ADMIN("admin"),
}

/** A pairing code to show as a QR code for [user] (More › Pair a device). */
data class Pairing(
    val user: User,
    /** Shown as text, e.g. `K7QM-4XPT`. */
    val code: String,
    /** Content of the QR code. */
    val uri: String,
    val expiresAt: Instant,
)
