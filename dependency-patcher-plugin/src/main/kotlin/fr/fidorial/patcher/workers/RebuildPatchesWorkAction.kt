package fr.fidorial.patcher.workers

import io.codechicken.diffpatch.cli.DiffOperation
import io.codechicken.diffpatch.util.Input
import io.codechicken.diffpatch.util.LogLevel
import io.codechicken.diffpatch.util.Output
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface RebuildPatchesParams : WorkParameters {
    val originalJar: RegularFileProperty
    val workspaceDir: DirectoryProperty
    val patchesDir: DirectoryProperty
    val context: Property<Int>
}

abstract class RebuildPatchesWorkAction : WorkAction<RebuildPatchesParams> {
    private val logger = Logging.getLogger(RebuildPatchesWorkAction::class.java)

    @OptIn(ExperimentalPathApi::class)
    override fun execute() {
        val params = parameters

        val originalPath = params.originalJar.get().asFile.toPath()
        val workspacePath = params.workspaceDir.get().asFile.toPath()
        val patchesPath = params.patchesDir.get().asFile.toPath()

        patchesPath.deleteRecursively()
        patchesPath.createDirectories()

        val operation = DiffOperation
            .builder()
            .logTo(logger::lifecycle)
            .level(LogLevel.INFO)
            .summary(false)
            .autoHeader(true)
            .context(params.context.get())
            .baseInput(Input.MultiInput.detectedArchive(originalPath))
            .changedInput(Input.MultiInput.folder(workspacePath))
            .patchesOutput(Output.MultiOutput.folder(patchesPath))
            .ignorePrefix("META-INF")
            .lineEnding("\n")
            .build()
            .operate()

        logger.lifecycle("Rebuilt ${operation.summary?.changedFiles} patches")
    }
}
