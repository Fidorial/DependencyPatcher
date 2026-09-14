import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.gradleup.shadow")
}

val shade = configurations.dependencyScope("shade")
val shadeResolvable = configurations.resolvable("shadeResolvable") {
    extendsFrom(shade)
}

configurations.implementation {
    extendsFrom(shade)
}

configurations.shadowRuntimeElements {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, named("9.4.0")) // we use lazy extendsFrom for configurations which was added in Gradle 9.4.0
    }
}

tasks.withType<ShadowJar>().configureEach {
    configurations.setFrom(listOf(shadeResolvable))
    archiveClassifier = ""
    configureRelocation()
    mergeServiceFiles()
    filesMatching("META-INF/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

private fun SetProperty<Configuration>.setFrom(configurations: List<NamedDomainObjectProvider<out Configuration>>) {
    empty()
    configurations.forEach { add(it) }
}

fun ShadowJar.configureRelocation() {
    val prefix = "fr.fidorial.patcher.libs"
    listOf(
        "io.codechicken.diffpatch",
        // transitive deps from diffpatch
        "net.covers1624.quack",
        "it.unimi.dsi",
        "org.apache.commons",
        "org.tukaani",
        "joptsimple",
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}
