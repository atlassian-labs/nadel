package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction

sealed interface NadelSchemaValidationResult

class NadelFieldResult(
    val service: Service,
    val fieldInstruction: NadelFieldInstruction,
) : NadelSchemaValidationResult
