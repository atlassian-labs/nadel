package graphql.nadel.tests.hooks

import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.transforms.RemoveFieldTestTransformForHierarchies


open class `base-hook` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
                RemoveFieldTestTransformForHierarchies()
        )
}

@UseHook
class `scopes-on-types-implementing-an-interface` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-1` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-2` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-3` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-4` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-5` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-6` : `base-hook`()

@UseHook
class `scopes-on-types-implementing-an-interface-7` : `base-hook`()