package graphql.nadel.tests.hooks

import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.transforms.RemoveFieldTestTransform

@UseHook
class `top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}

@UseHook
class `hydration-top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}

@UseHook
class `namespaced-hydration-top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}

@UseHook
class `hidden-namespaced-hydration-top-level-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}

@UseHook
class `namespaced-field-is-removed` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}

@UseHook
class `namespaced-field-is-removed-with-renames` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}

@UseHook
class `renamed-top-level-field-is-not-removed-short-circuit-hint-is-on` : EngineTestHook {
    override val customTransforms = listOf(RemoveFieldTestTransform())
}
