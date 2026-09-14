import org.gradle.plugin.compatibility.compatibility

plugins {
    `kotlin-dsl`
    id("fr.fidorial.patcher.spotless")
    id("fr.fidorial.patcher.shadow")
    id("com.gradle.plugin-publish") version "2.2.1"
    alias(libs.plugins.blossom)
}

repositories {
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

dependencies {
    shade(libs.diffpatch)
}

sourceSets.main {
    blossom.kotlinSources {
        property("diffpatch_version", libs.versions.diffpatch)
    }
}

gradlePlugin {
    plugins {
        register("dependencyPatcher") {
            id = "fr.fidorial.dependency-patcher"
            displayName = "Dependency Patcher"
            implementationClass = "fr.fidorial.patcher.DependencyPatcherPlugin"
            compatibility {
                features {
                    configurationCache = true
                    isolatedProjects = true
                }
            }
        }
    }
}
