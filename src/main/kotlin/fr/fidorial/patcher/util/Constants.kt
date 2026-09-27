package fr.fidorial.patcher.util

const val PLUGIN_NAME = "dependency patcher"
const val EXTENSION_NAME = "dependencyPatcher"

const val PATCHER_TASK_GROUP = PLUGIN_NAME
const val INTERNAL_PATCHER_TASK_GROUP = "$PLUGIN_NAME internal"

const val JAVA_PLUGIN_ID = "java"
const val PATCHED_CLASSIFIER = "patched"

const val DIFF_PATCH_COORDINATES_PREFIX = "io.codechicken:DiffPatch"

private const val CONFIG_PREFIX = EXTENSION_NAME

const val DIFF_PATCH_CONFIG_NAME = "${CONFIG_PREFIX}DiffPatchConfig"
const val DIFF_PATCH_RESOLVABLE_CONFIG_NAME = "${CONFIG_PREFIX}DiffPatchResolvableConfig"

private const val BUILD_OUTPUT_ROOT = "dependency-patcher"
private const val GENERATED_SOURCES_ROOT = "generated/sources/dependency-patcher"

private val NAME_SEPARATORS = Regex("[^A-Za-z0-9]+")

fun String.toLowerCamel(): String =
    split(NAME_SEPARATORS)
        .filter(String::isNotEmpty)
        .mapIndexed { index, part ->
            if (index == 0) part.replaceFirstChar(Char::lowercase) else part.replaceFirstChar(Char::uppercase)
        }.joinToString("")

fun String.toUpperCamel(): String = toLowerCamel().replaceFirstChar(Char::uppercase)

fun sourcesConfigName(patchSetName: String) = "$CONFIG_PREFIX${patchSetName.toUpperCamel()}SourcesConfig"

fun binaryConfigName(patchSetName: String) = "$CONFIG_PREFIX${patchSetName.toUpperCamel()}BinaryConfig"

fun workspaceSourceSetName(patchSetName: String) = "${patchSetName.toLowerCamel()}Workspace"

fun patchSourceSetName(patchSetName: String) = "${patchSetName.toLowerCamel()}Patch"

// Task name helpers accept either the raw patch set name or an already capitalized one.
fun applyPatchesTaskName(patchSetName: String) = "apply${patchSetName.toUpperCamel()}Patches"

fun setupWorkspaceTaskName(patchSetName: String) = "setup${patchSetName.toUpperCamel()}PatchWorkspace"

fun rebuildPatchesTaskName(patchSetName: String) = "rebuild${patchSetName.toUpperCamel()}Patches"

fun patchedJarTaskName(patchSetName: String) = "patched${patchSetName.toUpperCamel()}Jar"

fun extractPatchedFilesTaskName(patchSetName: String) = "extract${patchSetName.toUpperCamel()}PatchedFiles"

fun patchOutputDir(patchSetName: String) = "$BUILD_OUTPUT_ROOT/$patchSetName"

fun patchedZipPath(patchSetName: String) = "${patchOutputDir(patchSetName)}/patched.zip"

fun rejectsDirPath(patchSetName: String) = "${patchOutputDir(patchSetName)}/rejects"

fun generatedSourcesDir(patchSetName: String) = "$GENERATED_SOURCES_ROOT/$patchSetName"

fun patchDependenciesConfigName(patchSetName: String) = "${patchSetName.toLowerCamel()}PatchDependencies"
