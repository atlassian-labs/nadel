package graphql.nadel.engine.blueprint

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.EffectFieldArgumentDef
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
    val causeFieldDef: GraphQLFieldDefinition

    /**
     * The service that we will call to get the data for the hydration.
     */
    val effectService: Service

    /**
     * The query path to the field in the [effectService] to invoke for hydration e.g.
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
    val queryPathToEffectField: NadelQueryPath

    /**
     * Arguments needed to invoke [effectFieldDef].
     *
     * e.g. given
     *
     * ```graphql
     * type JiraQuery {
     *     issueById(id: ID!): JiraIssue
     * }
     * ```
     *
     * then the [EffectFieldArgumentDef] would be for the `id` argument.
     */
    val effectFieldArgDefs: List<EffectFieldArgumentDef>

    /**
     * Maximum time the client should wait for the hydration call before timing out.
     */
    val timeout: Int

    /**
     * The fields required to be queried on the source object in order to complete the hydration.
     * This can be the fields described in [EffectFieldArgumentDef.ValueSource.FromResultValue.queryPathToField]
     * or [NadelBatchHydrationMatchStrategy.MatchObjectIdentifier.sourceId].
     */
    val joiningFields: List<NadelQueryPath>

    /**
     * The field definition in the overall schema referenced by [queryPathToEffectField].
     */
    val effectFieldDef: GraphQLFieldDefinition

    /**
     * The container of the actor field in the overall schema referenced by [queryPathToEffectField].
     */
    val effectFieldContainer: GraphQLFieldsContainer
}

data class NadelHydrationFieldInstruction(
    // For documentation of override props see the parent
    override val location: FieldCoordinates,
    override val causeFieldDef: GraphQLFieldDefinition,
    override val effectService: Service,
    override val queryPathToEffectField: NadelQueryPath,
    override val effectFieldArgDefs: List<EffectFieldArgumentDef>,
    override val timeout: Int,
    override val joiningFields: List<NadelQueryPath>,
    override val effectFieldDef: GraphQLFieldDefinition,
    override val effectFieldContainer: GraphQLFieldsContainer,
    val hydrationStrategy: NadelHydrationStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelBatchHydrationFieldInstruction(
    // For documentation of override props see the parent
    override val location: FieldCoordinates,
    override val causeFieldDef: GraphQLFieldDefinition,
    override val effectService: Service,
    override val queryPathToEffectField: NadelQueryPath,
    override val effectFieldArgDefs: List<EffectFieldArgumentDef>,
    override val timeout: Int,
    override val joiningFields: List<NadelQueryPath>,
    override val effectFieldDef: GraphQLFieldDefinition,
    override val effectFieldContainer: GraphQLFieldsContainer,
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
