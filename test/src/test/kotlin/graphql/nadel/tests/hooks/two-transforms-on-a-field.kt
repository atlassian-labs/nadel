package graphql.nadel.tests.hooks

import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.transforms.RemoveFieldTestTransform

@KeepHook
class `two-transforms-on-a-field` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            RemoveFieldTestTransform()
        )
}
