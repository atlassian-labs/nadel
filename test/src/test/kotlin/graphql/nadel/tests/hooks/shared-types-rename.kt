package graphql.nadel.tests.hooks

import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

abstract class `shared-types-rename` : EngineTestHook

@UseHook
class `renamed-type-in-union-declared-in-another-service` : `shared-types-rename`()
