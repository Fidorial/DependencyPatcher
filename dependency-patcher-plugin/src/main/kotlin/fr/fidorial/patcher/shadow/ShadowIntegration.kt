package fr.fidorial.patcher.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import fr.fidorial.patcher.PatchSet
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register

internal object ShadowIntegration {
    fun registerPatchedJar(
        tasks: TaskContainer,
        name: String,
        patchSet: PatchSet,
        configure: Jar.() -> Unit,
    ): TaskProvider<out Jar> = tasks.register<ShadowJar>(name) {
        configure()
        configurations.set(emptyList())
        patchSet.relocations.get().forEach { (from, to) -> relocate(from, to) }
    }
}
