import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":app:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.ktor.client.core)
    implementation(libs.logback)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "cz.kotu.gamearena.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "cz.kotu.gamearena"
            packageVersion = "1.0.0"
        }
    }
}
