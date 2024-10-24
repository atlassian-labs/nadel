package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelTypeRenameInstruction

sealed interface NadelSchemaValidationResult {
    val isError: Boolean
}

internal class NadelValidatedTypeResult(
    val service: Service,
    val typeRenameInstruction: NadelTypeRenameInstruction,
) : NadelSchemaValidationResult {
    override val isError: Boolean = false
}

internal class NadelTypenamesForService(
    val service: Service,
    val underlyingTypeNames: Set<String>,
    val overallTypeNames: Set<String>,
) : NadelSchemaValidationResult {
    override val isError: Boolean = false
}

internal class NadelValidatedFieldResult(
    val service: Service,
    val fieldInstruction: NadelFieldInstruction,
) : NadelSchemaValidationResult {
    override val isError: Boolean = false
}
