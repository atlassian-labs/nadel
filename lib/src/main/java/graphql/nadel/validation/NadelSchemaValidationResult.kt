package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelFieldInstruction

sealed interface NadelSchemaValidationResult

class NadelFieldResult(
    val service: Service,
    val fieldInstruction: NadelFieldInstruction,
) : NadelSchemaValidationResult
