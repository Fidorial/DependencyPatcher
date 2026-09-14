plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.shadow)
    implementation(kotlin("gradle-plugin", embeddedKotlinVersion))
    implementation(libs.spotless)
}
