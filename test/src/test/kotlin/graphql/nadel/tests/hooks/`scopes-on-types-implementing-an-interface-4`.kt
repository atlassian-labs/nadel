package graphql.nadel.tests.hooks

import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.transforms.RemoveFieldTestTransformForHierarchies

@UseHook
class `scopes-on-types-implementing-an-interface-4` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            RemoveFieldTestTransformForHierarchies()
        )
}
