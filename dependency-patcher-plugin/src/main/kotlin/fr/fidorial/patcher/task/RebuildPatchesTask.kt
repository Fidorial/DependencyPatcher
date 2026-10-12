package fr.fidorial.patcher.task

import fr.fidorial.patcher.workers.RebuildPatchesWorkAction
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor

@DisableCachingByDefault(because = "Diffing is cheap")
abstract class RebuildPatchesTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    abstract val originalJar: RegularFileProperty

    @get:InputDirectory
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val workspaceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val patchesDir: DirectoryProperty

    @get:Input
    abstract val context: Property<Int>

    @get:Classpath
    abstract val toolClasspath: ConfigurableFileCollection

    @TaskAction
    fun rebuild() {
        if (!workspaceDir.isPresent) return

        workerExecutor
            .classLoaderIsolation { classpath.from(toolClasspath) }
            .submit(RebuildPatchesWorkAction::class.java) {
                originalJar.set(this@RebuildPatchesTask.originalJar)
                workspaceDir.set(this@RebuildPatchesTask.workspaceDir)
                patchesDir.set(this@RebuildPatchesTask.patchesDir)
                context.set(this@RebuildPatchesTask.context)
            }
    }
}
