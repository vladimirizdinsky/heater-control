package io.github.vladimirizdinsky.heatercontrol.feature.users.data

import io.github.vladimirizdinsky.heatercontrol.core.network.apiEnumOf
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.PairingDto
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.UserDto
import io.github.vladimirizdinsky.heatercontrol.feature.users.domain.model.Pairing
import io.github.vladimirizdinsky.heatercontrol.feature.users.domain.model.User
import io.github.vladimirizdinsky.heatercontrol.feature.users.domain.model.UserRole
import java.time.Instant

fun UserDto.toDomain() = User(
    id = id,
    name = name,
    // An unknown role gets the fewer rights.
    role = apiEnumOf(role) ?: UserRole.USER,
)

fun PairingDto.toDomain() = Pairing(
    user = user.toDomain(),
    code = code,
    uri = uri,
    expiresAt = Instant.parse(expiresAt),
)
