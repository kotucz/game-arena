plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.ksp)
}

group = "cz.kotu.gamearena"
version = "1.0.0"
application {
    mainClass = "cz.kotu.gamearena.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.sessions)
    ksp(libs.room.compiler)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
