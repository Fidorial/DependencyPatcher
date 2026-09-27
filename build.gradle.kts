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

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "dependency-patcher"
            url = "https://github.com/Fidorial/DependencyPatcher"
            licenses {
                license {
                    name = "MIT License"
                    url = "https://opensource.org/license/mit"
                }
            }
            developers {
                developer {
                    id = "fidorial"
                    name = "Fidorial"
                }
            }
            scm {
                url = "https://github.com/Fidorial/DependencyPatcher"
                connection = "scm:git:https://github.com/Fidorial/DependencyPatcher.git"
                developerConnection = "scm:git:ssh://git@github.com/Fidorial/DependencyPatcher.git"
            }
        }
    }

    repositories {
        maven {
            name = "Euphyllia"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) {
                    "https://repo.euphyllia.moe/repository/maven-snapshots/"
                } else {
                    "https://repo.euphyllia.moe/repository/maven-releases/"
                },
            )
            credentials {
                username = providers.environmentVariable("NEXUS_USERNAME").orNull ?: ""
                password = providers.environmentVariable("NEXUS_PASSWORD").orNull ?: ""
            }
        }
    }
}