package io.github.vladimirizdinsky.heatercontrol.core.network

/**
 * The server answered with an error status (heater-hub `docs/api.md`, section 6).
 *
 * [code] is `null` when the body is not a heater-hub error, e.g. another server on that
 * address. Network failures are not wrapped: they stay `java.io.IOException`.
 */
class ApiException(
    val status: Int,
    val code: String?,
    /** Developer text from the server, for logs only. */
    serverMessage: String? = null,
) : Exception("HTTP $status ${code ?: "(no error code)"}" + (serverMessage?.let { ": $it" } ?: "")) {

    /** Error codes the app reacts to; any other code is handled as a generic error. */
    companion object Codes {
        const val BAD_REQUEST = "BAD_REQUEST"
        const val UNKNOWN_FUNCTION = "UNKNOWN_FUNCTION"
        const val INVALID_VALUE = "INVALID_VALUE"
        const val PAIRING_CODE_INVALID = "PAIRING_CODE_INVALID"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val FORBIDDEN = "FORBIDDEN"
        const val DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND"
        const val COMMAND_NOT_FOUND = "COMMAND_NOT_FOUND"
        const val USER_NOT_FOUND = "USER_NOT_FOUND"
        const val USER_EXISTS = "USER_EXISTS"
        const val DEVICE_OFFLINE = "DEVICE_OFFLINE"
        const val FUNCTION_UNAVAILABLE = "FUNCTION_UNAVAILABLE"
        const val TOO_MANY_ATTEMPTS = "TOO_MANY_ATTEMPTS"
    }
}
