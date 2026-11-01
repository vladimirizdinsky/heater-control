package io.github.vladimirizdinsky.heatercontrol.core.network

import kotlin.enums.enumEntries

/** An enum whose entries are sent by the server as [apiId] (heater-hub `docs/api.md`, section 7). */
interface ApiEnum {
    val apiId: String
}

/** The entry of [T] with this [apiId], or `null` if the app does not know it (section 8). */
inline fun <reified T> apiEnumOf(apiId: String?): T? where T : Enum<T>, T : ApiEnum =
    apiId?.let { id -> enumEntries<T>().firstOrNull { it.apiId == id } }
