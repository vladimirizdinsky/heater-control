plugins {
    alias(libs.plugins.heatercontrol.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.vladimirizdinsky.heatercontrol.core.network"
}

dependencies {
    // api: feature modules call HttpClient and its plugins directly.
    api(libs.ktor.client.core)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.okhttp)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
