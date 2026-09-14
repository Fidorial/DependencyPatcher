dependency-patcher
=========
dependency-patcher is a Gradle plugin for patching dependencies. Patches are diffed and applied against the dependency's own published sources, then recompiled and repacked back into the original jar.

## Usage
Add the maven repository to your `settings.gradle[.kts]` file:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.euphyllia.moe/repository/maven-public/")
    }
}
```

Apply the plugin to your project. It requires a minimum of Gradle 9.4 and Java 17.

```kotlin
plugins {
  id("fr.fidorial.dependency-patcher") version "<version>"
}
```

Declare one or more patch sets via the `dependencyPatcher` extension:

```kotlin
dependencyPatcher {
  patchSet("example") {
    library("com.example:example-lib:1.0.0")
    // or, from a version catalog:
    library.set(libs.exampleLib)
  }
}
```

Patches from `patches/example` are applied against the dependency's sources, recompiled and repacked over the original jar's classes into a `patchedExampleJar`, then added to `implementation`/`testImplementation` by default.

> [!IMPORTANT]
> The dependency must publish a `sources` jar (or another classifier, via the `classifier` property) as the plugin is only capable of patching source files.

> [!NOTE]
> The `exampleWorkspace` source set isn't populated by default. Run `setup<Name>PatchWorkspace` to populate it.

### Restrictions

- **Java sources only.** Only `.java` files under the workspace source set are diffed, patched, and recompiled. Other JVM languages (Kotlin, Scala, Groovy, etc.) in the dependency's sources are not supported.
- **No `META-INF` patching.** Manifest entries, service files, and other `META-INF` contents are carried over from the original jar unchanged and cannot be patched.

### JPMS

If the patched dependency is a named module, set `module` (and optionally `joinedModule`, which defaults to `ALL-UNNAMED`) on the patch set to have the compiler treat it as a patched module (`--patch-module`) rather than a plain classpath entry.

### IDE Support

IDEs typically don't pick up patch changes automatically on sync. To see them reflected, compile the project first, then resync.

### Rebuilding patches

After editing files in a patch set's workspace directory, run `rebuild<Name>Patches` to diff the workspace against the original sources and regenerate the patch files.
You can also set `autoRebuild = true` on a patch set to have compilation rebuild from the current workspace state on every run; useful for development.

## Tasks

> [!TIP]
> The `apply<Name>Patches` tasks are automatically attached to the compilation process, so you don't need to run them manually.

Per patch set `<name>` (capitalized, e.g. `Example`), the plugin registers:

- `apply<Name>Patches` — applies patches to the original sources
- `setup<Name>PatchWorkspace` — extracts the patched sources into the `<name>Workspace` source set for editing
- `rebuild<Name>Patches` — diffs the workspace against the original sources and outputs patches

See [`PatchSet`](src/main/kotlin/fr/fidorial/patcher/PatchSet.kt) for the full set of configurable options.
