package graphql.nadel.tests.hooks

import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.transforms.RemoveFieldTestTransform

@UseHook
class `two-transforms-on-a-field` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            RemoveFieldTestTransform()
        )
}
