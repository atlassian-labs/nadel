package graphql.nadel.engine.blueprint

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer

sealed class NadelFieldInstruction {
    /**
     * The coordinates of the field that this [NadelFieldInstruction] is for.
     */
    abstract val location: FieldCoordinates
}

/**
 *
 */
interface NadelGenericHydrationInstruction {
    /**
     * The field to be hydrated i.e. the field with the @hydrated directive on it.
     */
    val virtualFieldDef: GraphQLFieldDefinition

    /**
     * The service that we will call to get the data for the hydration.
     */
    val backingService: Service

    /**
     * The query path to the field in the [backingService] to invoke for hydration e.g.
     *
     * ```graphql
     * type Query {
     *     jira: JiraQuery
     * }
     * type JiraQuery {
     *     issueById(id: ID!): JiraIssue
     * }
     * ```
     *
     * then the query path is `NadelQueryPath(segments = [jira, issueById])`.
     */
    val queryPathToBackingField: NadelQueryPath

    /**
     * Arguments needed to invoke [backingFieldDef].
     *
     * e.g. given
     *
     * ```graphql
     * type JiraQuery {
     *     issueById(id: ID!): JiraIssue
     * }
     * ```
     *
     * then the [NadelHydrationArgument] would be for the `id` argument.
     */
    val backingFieldArguments: List<NadelHydrationArgument>

    /**
     * Maximum time the client should wait for the hydration call before timing out.
     */
    val timeout: Int

    /**
     * The fields required to be queried on the source object in order to complete the hydration.
     * This can be the fields described in [NadelHydrationArgument.SourceField.pathToSourceField]
     * or [NadelBatchHydrationMatchStrategy.MatchObjectIdentifier.sourceId].
     */
    val sourceFields: List<NadelQueryPath>

    /**
     * The field definition in the overall schema referenced by [queryPathToBackingField].
     */
    val backingFieldDef: GraphQLFieldDefinition

    /**
     * The container of the backing field in the overall schema referenced by [queryPathToBackingField].
     */
    val backingFieldContainer: GraphQLFieldsContainer

    /**
     * The optional definition for conditional hydrations
     */
    val condition: NadelHydrationCondition?
}

data class NadelHydrationFieldInstruction(
    // For documentation of override props see the parent
    override val location: FieldCoordinates,
    override val virtualFieldDef: GraphQLFieldDefinition,
    override val backingService: Service,
    override val queryPathToBackingField: NadelQueryPath,
    override val backingFieldArguments: List<NadelHydrationArgument>,
    override val timeout: Int,
    override val sourceFields: List<NadelQueryPath>,
    override val backingFieldDef: GraphQLFieldDefinition,
    override val backingFieldContainer: GraphQLFieldsContainer,
    override val condition: NadelHydrationCondition?,
    /**
     * Hydration can bring about virtual types.
     *
     * These types mirror the backing type, but can have things like hydration etc.
     *
     * This context provides a mapping between the virtual and backing types.
     */
    val virtualTypeContext: NadelVirtualTypeContext?,
    val hydrationStrategy: NadelHydrationStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelBatchHydrationFieldInstruction(
    // For documentation of override props see the parent
    override val location: FieldCoordinates,
    override val virtualFieldDef: GraphQLFieldDefinition,
    override val backingService: Service,
    override val queryPathToBackingField: NadelQueryPath,
    override val backingFieldArguments: List<NadelHydrationArgument>,
    override val timeout: Int,
    override val sourceFields: List<NadelQueryPath>,
    override val backingFieldDef: GraphQLFieldDefinition,
    override val backingFieldContainer: GraphQLFieldsContainer,
    override val condition: NadelHydrationCondition?,
    val batchSize: Int,
    val batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelDeepRenameFieldInstruction(
    override val location: FieldCoordinates,
    val queryPathToField: NadelQueryPath,
) : NadelFieldInstruction()

data class NadelRenameFieldInstruction(
    override val location: FieldCoordinates,
    val underlyingName: String,
) : NadelFieldInstruction() {
    val overallName: String get() = location.fieldName
}

data class NadelPartitionInstruction(
    override val location: FieldCoordinates,
    val pathToPartitionArg: List<String>,
) : NadelFieldInstruction()
