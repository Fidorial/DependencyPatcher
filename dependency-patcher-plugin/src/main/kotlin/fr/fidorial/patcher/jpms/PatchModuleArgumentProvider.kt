package fr.fidorial.patcher.jpms

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider

abstract class PatchModuleArgumentProvider : CommandLineArgumentProvider {
    @get:Input
    @get:Optional
    abstract val module: Property<String>

    @get:Input
    @get:Optional
    abstract val joinedModule: Property<String>

    @get:Classpath
    abstract val patchModule: RegularFileProperty

    override fun asArguments(): Iterable<String> {
        if (!module.isPresent) {
            return emptyList()
        }

        val moduleName = module.get()

        return buildList {
            add("--patch-module")
            add("$moduleName=${patchModule.get().asFile.absolutePath}")

            add("--add-reads")
            add("$moduleName=${joinedModule.orNull ?: "ALL-UNNAMED"}")
        }
    }
}
