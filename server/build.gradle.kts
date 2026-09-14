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
    implementation(libs.ktor.server.html.builder)
    implementation(libs.kotlin.inject.runtime.kmp)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.napier)
    ksp(libs.room.compiler)
    ksp(libs.kotlin.inject.compiler.ksp)
    kspTest(libs.kotlin.inject.compiler.ksp)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
