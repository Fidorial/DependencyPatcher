package fr.fidorial.patcher

import io.codechicken.diffpatch.match.FuzzyLineMatcher
import io.codechicken.diffpatch.util.PatchMode
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

abstract class PatchSet
    @Inject
    constructor(
        private val patchSetName: String,
        private val dependencyFactory: DependencyFactory,
        configurations: ConfigurationContainer,
        objects: ObjectFactory,
        projectLayout: ProjectLayout,
    ) : Named {
        override fun getName(): String = patchSetName

        /**
         * The dependency to be patched.
         *
         * Accepts a version catalog entry directly, or a plain string notation via
         * the [library] the function below.
         */
        abstract val library: Property<ExternalModuleDependency>

        /**
         * Sets [library] from a plain dependency notation.
         */
        fun library(dependencyNotation: CharSequence) {
            library.set(dependencyFactory.create(dependencyNotation))
        }

        /**
         * The JPMS module being patched.
         *
         * When left unconfigured, JPMS patching is disabled for this patch set.
         */
        abstract val module: Property<String>

        /**
         * The module to which the patched module should have read access.
         *
         * Defaults to `ALL-UNNAMED` when [module] is configured.
         */
        abstract val joinedModule: Property<String>

        /**
         * The classifier for the artifact containing the dependency's sources.
         */
        val classifier: Property<String> = objects.property<String>().convention("sources")

        /**
         * The directory in which patches should be stored.
         */
        val patchesDir: DirectoryProperty =
            objects.directoryProperty().convention(projectLayout.projectDirectory.dir("patches/$patchSetName"))

        /**
         * The directory containing the extracted library sources used as the patching workspace.
         */
        val workspaceDir: DirectoryProperty =
            objects.directoryProperty().convention(projectLayout.buildDirectory.dir("workspace/$patchSetName"))

        /**
         * The patch mode used for applying patches.
         *
         * @see PatchMode
         */
        val patchMode: Property<PatchMode> = objects.property<PatchMode>().convention(PatchMode.OFFSET)

        /**
         * The minimum similarity for a patch to be applied, used when the patch mode is set to [PatchMode.FUZZY].
         */
        val minFuzz: Property<Float> = objects.property<Float>().convention(FuzzyLineMatcher.DEFAULT_MIN_MATCH_SCORE)

        /**
         * The maximum accepted line offset for a patch to be applied, used when the patch mode is set to
         * [PatchMode.OFFSET] or [PatchMode.FUZZY].
         */
        val maxOffset: Property<Int> = objects.property<Int>().convention(FuzzyLineMatcher.MatchMatrix.DEFAULT_MAX_OFFSET)

        /**
         * The number of context lines included in generated patches.
         */
        val context: Property<Int> = objects.property<Int>().convention(3)

        /**
         * The configuration providers to which the patched dependency should be added.
         */
        val targetConfigurations =
            objects.setProperty<NamedDomainObjectProvider<out Configuration>>().convention(
                setOf(
                    configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME),
                    configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME),
                ),
            )

        /**
         * Whether compilation should automatically rebuild patches when a workspace has been generated.
         *
         * Useful for testing changes during development, as only changes saved to patch files are used during compilation.
         */
        val autoRebuild: Property<Boolean> = objects.property<Boolean>().convention(false)

        /**
         * Dependencies needed to compile this patch set's patched sources and its workspace.
         *
         * The patched library itself is resolved non-transitively, and main's classpath cannot be reused
         * because it contains the patched jars, so anything the patched sources import must be declared here.
         */
        @get:Nested
        abstract val dependencies: PatchSetDependencies

        /**
         * Configures the [dependencies] of this patch set.
         *
         */
        fun dependencies(action: Action<in PatchSetDependencies>) = action.execute(dependencies)
    }

/**
 * Dependency buckets available in a patch set's `dependencies {}` block.
 */
interface PatchSetDependencies : Dependencies {
    /**
     * Dependencies on the compile classpath of the patched sources and the workspace only.
     * They are not added to the consuming project's classpath.
     */
    val compileOnly: DependencyCollector
}
