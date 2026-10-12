package fr.fidorial.patcher.extension

import fr.fidorial.patcher.PatchSet
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory

abstract class DependencyPatcherExtension
@Inject
constructor(
    objects: ObjectFactory,
) {
    val patchSets: NamedDomainObjectContainer<PatchSet> =
        objects.domainObjectContainer(PatchSet::class.java) { name -> objects.newInstance(PatchSet::class.java, name) }

    fun patchSet(
        name: String,
        configure: PatchSet.() -> Unit,
    ) {
        patchSets.maybeCreate(name)
        patchSets.named(name, configure)
    }
}
