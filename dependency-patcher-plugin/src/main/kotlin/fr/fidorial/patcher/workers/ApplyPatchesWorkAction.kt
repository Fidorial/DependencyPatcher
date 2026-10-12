package fr.fidorial.patcher.workers

import io.codechicken.diffpatch.cli.PatchOperation
import io.codechicken.diffpatch.util.Input
import io.codechicken.diffpatch.util.LogLevel
import io.codechicken.diffpatch.util.Output
import io.codechicken.diffpatch.util.PatchMode
import io.codechicken.diffpatch.util.archiver.ArchiveFormat
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface ApplyPatchesParams : WorkParameters {
    val originalJar: RegularFileProperty
    val patchesDir: DirectoryProperty
    val outputZip: RegularFileProperty
    val rejectsDir: DirectoryProperty
    val mode: Property<PatchMode>
    val minFuzz: Property<Float>
    val maxOffset: Property<Int>
}

abstract class ApplyPatchesWorkAction : WorkAction<ApplyPatchesParams> {
    private val logger = Logging.getLogger(ApplyPatchesWorkAction::class.java)

    override fun execute() {
        val params = parameters

        val originalPath = params.originalJar.get().asFile.toPath()
        val patchesPath = params.patchesDir.get().asFile.toPath()
        val outputPath = params.outputZip.get().asFile.toPath()
        val rejectsPath = params.rejectsDir.get().asFile.toPath()

        val operation = PatchOperation
            .builder()
            .logTo(logger::lifecycle)
            .level(LogLevel.INFO)
            .summary(false)
            .baseInput(Input.MultiInput.archive(ArchiveFormat.ZIP, originalPath))
            .patchesInput(Input.MultiInput.folder(patchesPath))
            .patchedOutput(Output.MultiOutput.archive(ArchiveFormat.ZIP, outputPath))
            .rejectsOutput(Output.MultiOutput.folder(rejectsPath))
            .mode(params.mode.get())
            .minFuzz(params.minFuzz.get())
            .maxOffset(params.maxOffset.get())
            .ignorePrefix("META-INF")
            .lineEnding("\n")
            .build()
            .operate()

        if (operation.exit != 0) {
            throw GradleException("Failed to apply patches from '$patchesPath' onto '$originalPath'. See rejected hunks in '$rejectsPath'.")
        }

        logger.lifecycle("Applied ${operation.summary?.changedFiles} patches")
    }
}
