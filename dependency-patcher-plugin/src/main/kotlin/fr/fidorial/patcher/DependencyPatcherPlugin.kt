package fr.fidorial.patcher

import fr.fidorial.patcher.extension.DependencyPatcherExtension
import fr.fidorial.patcher.jpms.PatchModuleArgumentProvider
import fr.fidorial.patcher.task.ApplyPatchesTask
import fr.fidorial.patcher.task.ExtractPatchedFilesTask
import fr.fidorial.patcher.task.RebuildPatchesTask
import fr.fidorial.patcher.util.DIFF_PATCH_CONFIG_NAME
import fr.fidorial.patcher.util.DIFF_PATCH_COORDINATES_PREFIX
import fr.fidorial.patcher.util.DIFF_PATCH_RESOLVABLE_CONFIG_NAME
import fr.fidorial.patcher.util.EXTENSION_NAME
import fr.fidorial.patcher.util.INTERNAL_PATCHER_TASK_GROUP
import fr.fidorial.patcher.util.JAVA_PLUGIN_ID
import fr.fidorial.patcher.util.LibraryVersions
import fr.fidorial.patcher.util.PATCHED_CLASSIFIER
import fr.fidorial.patcher.util.PATCHER_TASK_GROUP
import fr.fidorial.patcher.util.applyPatchesTaskName
import fr.fidorial.patcher.util.binaryConfigName
import fr.fidorial.patcher.util.cleanWorkspaceTaskName
import fr.fidorial.patcher.util.extractPatchedFilesTaskName
import fr.fidorial.patcher.util.generatedSourcesDir
import fr.fidorial.patcher.util.patchDependenciesConfigName
import fr.fidorial.patcher.util.patchOutputDir
import fr.fidorial.patcher.util.patchSourceSetName
import fr.fidorial.patcher.util.patchedJarTaskName
import fr.fidorial.patcher.util.patchedZipPath
import fr.fidorial.patcher.util.rebuildPatchesTaskName
import fr.fidorial.patcher.util.rejectsDirPath
import fr.fidorial.patcher.util.setupWorkspaceTaskName
import fr.fidorial.patcher.util.sourcesConfigName
import fr.fidorial.patcher.util.toUpperCamel
import fr.fidorial.patcher.util.workspaceSourceSetName
import javax.inject.Inject
import kotlin.collections.forEach
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencyScopeConfiguration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register

abstract class DependencyPatcherPlugin : Plugin<Project> {
    @get:Inject
    protected abstract val configurations: ConfigurationContainer

    @get:Inject
    protected abstract val tasks: TaskContainer

    @get:Inject
    protected abstract val layout: ProjectLayout

    override fun apply(project: Project) {
        val extension = project.extensions.create<DependencyPatcherExtension>(EXTENSION_NAME)

        val diffPatch =
            configurations.dependencyScope(DIFF_PATCH_CONFIG_NAME) {
                defaultDependencies {
                    dependencies.add(
                        project.dependencies.create("${DIFF_PATCH_COORDINATES_PREFIX}:${LibraryVersions.DIFF_PATCH}")
                    )
                }
            }

        val diffPatchTool =
            configurations.resolvable(DIFF_PATCH_RESOLVABLE_CONFIG_NAME) {
                extendsFrom(diffPatch)
            }

        val applyAllPatches = tasks.register("applyAllPatches") {
            group = PATCHER_TASK_GROUP
            description = "Applies patches for all patch sets."
        }

        val rebuildAllPatches = tasks.register("rebuildAllPatches") {
            group = PATCHER_TASK_GROUP
            description = "Rebuilds patches for all patch sets."
        }

        val setupAllWorkspaces = tasks.register("setupAllWorkspaces") {
            group = PATCHER_TASK_GROUP
            description = "Builds workspaces for all patch sets."
        }

        val cleanAllWorkspaces = tasks.register("cleanAllWorkspaces") {
            group = PATCHER_TASK_GROUP
            description = "Delete all patch workspaces."
        }

        extension.patchSets.configureEach {
            configure(project, diffPatchTool, applyAllPatches, rebuildAllPatches, setupAllWorkspaces, cleanAllWorkspaces)
        }
    }

    private fun PatchSet.configure(
        project: Project,
        diffPatchTool: NamedDomainObjectProvider<out Configuration>,
        applyAllPatches: TaskProvider<Task>,
        rebuildAllPatches: TaskProvider<Task>,
        setupAllWorkspaces: TaskProvider<Task>,
        cleanAllWorkspaces: TaskProvider<Task>,
    ) {
        val capitalized = name.toUpperCamel()

        val originalSources = configurations.resolvable(sourcesConfigName(name)) {
            isTransitive = false
            dependencies.addLater(
                library.map {
                    project.dependencies.create(
                        it.copy().apply {
                            artifact {
                                classifier = this@configure.classifier.get()
                                type = "jar"
                            }
                        }
                    )
                }
            )
        }

        val originalBinary = configurations.resolvable(binaryConfigName(name)) {
            isTransitive = false
            dependencies.addLater(
                library.map {
                    project.dependencies.create(it)
                }
            )
        }

        val originalSourcesJar = layout.file(
            originalSources.flatMap { it.elements.map { it.single().asFile } }
        )

        val originalBinaryJar = layout.file(
            originalBinary.flatMap { it.elements.map { it.single().asFile } }
        )

        val patchDependencies = configurations.dependencyScope(patchDependenciesConfigName(name)) {
            description = "Dependencies needed to compile the patched sources of '${this@configure.name}'."
            fromDependencyCollector(this@configure.dependencies.compileOnly)
        }

        val cleanWorkspace = tasks.register<Delete>(cleanWorkspaceTaskName(capitalized)) {
            group = PATCHER_TASK_GROUP
            description = "Delete the '${this@configure.name}' patch workspace."
            delete(this@configure.workspaceDir)
        }

        val patchedZip = layout.buildDirectory.file(patchedZipPath(name))
        val rejects = layout.buildDirectory.dir(rejectsDirPath(name))

        val rebuildPatches = tasks.register<RebuildPatchesTask>(rebuildPatchesTaskName(capitalized)) {
            group = PATCHER_TASK_GROUP
            description = "Diffs the '${this@configure.name}' patch workspace against the original sources and generates patches."
            originalJar.set(originalSourcesJar)
            workspaceDir.set(
                autoRebuild.flatMap { enabled ->
                    if (enabled) {
                        this@configure.workspaceDir.filter { it.asFile.exists() }
                    } else {
                        this@configure.workspaceDir
                    }
                }
            )
            patchesDir.set(this@configure.patchesDir)
            context.set(this@configure.context)
            toolClasspath.from(diffPatchTool)
        }

        val applyPatches = tasks.register<ApplyPatchesTask>(applyPatchesTaskName(capitalized)) {
            group = PATCHER_TASK_GROUP
            description = "Applies patches for '${this@configure.name}'."

            originalJar.set(originalSourcesJar)
            patchesDir.set(
                autoRebuild.flatMap { enabled ->
                    if (enabled) {
                        rebuildPatches.flatMap { it.patchesDir }
                    } else {
                        this@configure.patchesDir.filter { it.asFile.exists() }
                    }
                }
            )
            outputZip.set(patchedZip)
            rejectsDir.set(rejects)
            mode.set(patchMode)
            minFuzz.set(this@configure.minFuzz)
            maxOffset.set(this@configure.maxOffset)

            toolClasspath.from(diffPatchTool)
        }

        val setupWorkspace = tasks.register<Sync>(setupWorkspaceTaskName(capitalized)) {
            group = PATCHER_TASK_GROUP
            description = "Builds a workspace for '${this@configure.name}' from the current patched source."

            from(project.zipTree(applyPatches.flatMap { it.outputZip }))
            into(this@configure.workspaceDir)
        }

        applyAllPatches { dependsOn(applyPatches) }
        rebuildAllPatches { dependsOn(rebuildPatches) }
        setupAllWorkspaces { dependsOn(setupWorkspace) }
        cleanAllWorkspaces { dependsOn(cleanWorkspace) }

        project.pluginManager.withPlugin(JAVA_PLUGIN_ID) {
            configureJava(
                project = project,
                patchSet = this@configure,
                capitalized = capitalized,
                applyPatches = applyPatches,
                setupWorkspace = setupWorkspace,
                originalBinaryJar = originalBinaryJar,
                patchDependenciesConfiguration = patchDependencies
            )
        }
    }

    private fun configureJava(
        project: Project,
        patchSet: PatchSet,
        capitalized: String,
        applyPatches: TaskProvider<ApplyPatchesTask>,
        setupWorkspace: TaskProvider<Sync>,
        originalBinaryJar: Provider<RegularFile>,
        patchDependenciesConfiguration: NamedDomainObjectProvider<DependencyScopeConfiguration>,
    ) {
        val sourceSets = project.extensions.getByType<SourceSetContainer>()

        sourceSets.register(workspaceSourceSetName(patchSet.name)) {
            java.srcDir(setupWorkspace.map { it.destinationDir })
            configurations.named(compileOnlyConfigurationName) {
                extendsFrom(patchDependenciesConfiguration)
            }
        }

        val extractPatchedFiles = tasks.register<ExtractPatchedFilesTask>(extractPatchedFilesTaskName(patchSet.name)) {
            group = INTERNAL_PATCHER_TASK_GROUP
            description = "Extracts changed files for '${patchSet.name}'."
            patchedZip.set(applyPatches.flatMap { it.outputZip })
            patchesDir.set(patchSet.patchesDir)
            outputDir.set(layout.buildDirectory.dir(generatedSourcesDir(patchSet.name)))
        }

        val patchSourceSet = sourceSets.register(patchSourceSetName(patchSet.name)) {
            java.srcDir(extractPatchedFiles.flatMap { it.outputDir })
            if (!patchSet.module.isPresent) {
                compileClasspath += layout.files(originalBinaryJar)
            }
            configurations.named(compileOnlyConfigurationName) {
                extendsFrom(patchDependenciesConfiguration)
            }
        }

        patchSourceSet.configure {
            tasks.named<JavaCompile>(compileJavaTaskName) {
                val provider = project.objects.newInstance<PatchModuleArgumentProvider>()

                provider.module.set(patchSet.module)
                provider.patchModule.set(originalBinaryJar)
                provider.joinedModule.set(patchSet.joinedModule)
                options.compilerArgumentProviders.add(provider)
            }
        }

        val patchedJar = tasks.register<Jar>(patchedJarTaskName(capitalized)) {
            group = INTERNAL_PATCHER_TASK_GROUP

            archiveBaseName.set(patchSet.name)
            archiveClassifier.set(PATCHED_CLASSIFIER)
            destinationDirectory.set(layout.buildDirectory.dir(patchOutputDir(patchSet.name)))
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(patchSourceSet.map { it.output })
            from(project.zipTree(originalBinaryJar)) {
                // we can never match the original signature
                exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.EC", "META-INF/SIG-*")
            }
        }

        patchSet.targetConfigurations.get().forEach { config ->
            config.configure {
                dependencies.add(
                    project.dependencies.create(layout.files(patchedJar))
                )
            }
        }
    }
}
