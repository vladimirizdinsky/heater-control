plugins {
    alias(libs.plugins.heatercontrol.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.vladimirizdinsky.heatercontrol.feature.users"
}

dependencies {
    implementation(projects.core.network)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
