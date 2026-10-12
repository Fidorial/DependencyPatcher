package fr.fidorial.patcher.task

import fr.fidorial.patcher.workers.ApplyPatchesWorkAction
import io.codechicken.diffpatch.util.PatchMode
import javax.inject.Inject
import kotlin.io.path.copyTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class ApplyPatchesTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    abstract val originalJar: RegularFileProperty

    @get:InputDirectory
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val patchesDir: DirectoryProperty

    @get:OutputFile
    abstract val outputZip: RegularFileProperty

    @get:OutputDirectory
    abstract val rejectsDir: DirectoryProperty

    @get:Input
    abstract val mode: Property<PatchMode>

    @get:Input
    abstract val minFuzz: Property<Float>

    @get:Input
    abstract val maxOffset: Property<Int>

    @get:Classpath
    abstract val toolClasspath: ConfigurableFileCollection

    @TaskAction
    fun apply() {
        val original = originalJar.get().asFile
        val output = outputZip.get().asFile
        val patches = patchesDir.orNull?.asFile

        if (patches == null) {
            original.toPath().copyTo(output.toPath(), overwrite = true)
            return
        }

        workerExecutor
            .classLoaderIsolation { classpath.from(toolClasspath) }
            .submit(ApplyPatchesWorkAction::class.java) {
                originalJar.set(this@ApplyPatchesTask.originalJar)
                patchesDir.set(this@ApplyPatchesTask.patchesDir)
                outputZip.set(this@ApplyPatchesTask.outputZip)
                rejectsDir.set(this@ApplyPatchesTask.rejectsDir)
                mode.set(this@ApplyPatchesTask.mode)
                minFuzz.set(this@ApplyPatchesTask.minFuzz)
                maxOffset.set(this@ApplyPatchesTask.maxOffset)
            }
    }
}
