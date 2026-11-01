package io.github.vladimirizdinsky.heatercontrol.feature.users.data

import io.github.vladimirizdinsky.heatercontrol.core.network.HttpClientFactory
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.PairingDto
import io.github.vladimirizdinsky.heatercontrol.feature.users.data.dto.UserDto
import io.github.vladimirizdinsky.heatercontrol.feature.users.domain.model.User
import io.github.vladimirizdinsky.heatercontrol.feature.users.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class UserMapperTest {

    @Test
    fun `maps a user with a known role`() {
        val user = json.decodeFromString<UserDto>("""{ "id": "jano", "name": "Jano", "role": "admin" }""").toDomain()

        assertEquals(User(id = "jano", name = "Jano", role = UserRole.ADMIN), user)
    }

    @Test
    fun `an unknown role gets the fewer rights`() {
        val user = json.decodeFromString<UserDto>("""{ "id": "eva", "name": "Eva", "role": "guest" }""").toDomain()

        assertEquals(UserRole.USER, user.role)
    }

    @Test
    fun `maps a pairing code`() {
        val pairing = json.decodeFromString<PairingDto>(PAIRING).toDomain()

        assertEquals(User(id = "anna", name = "Anna", role = UserRole.USER), pairing.user)
        assertEquals("K7QM-4XPT", pairing.code)
        assertEquals("heaterhub://pair?server=http%3A%2F%2F192.168.0.39%3A8080&code=K7QM4XPT", pairing.uri)
        assertEquals(Instant.parse("2026-10-28T12:10:00Z"), pairing.expiresAt)
    }

    private companion object {
        val json = HttpClientFactory.json

        /** `docs/api.md`, section 5.0, with `qrSvg` that the app ignores. */
        const val PAIRING = """
            { "id": "5c01e7aa", "user": { "id": "anna", "name": "Anna", "role": "user" },
              "code": "K7QM-4XPT",
              "uri": "heaterhub://pair?server=http%3A%2F%2F192.168.0.39%3A8080&code=K7QM4XPT",
              "expiresAt": "2026-10-28T12:10:00Z",
              "qrSvg": "<svg></svg>" }
        """
    }
}
