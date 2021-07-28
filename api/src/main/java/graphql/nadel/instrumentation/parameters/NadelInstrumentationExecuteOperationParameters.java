package graphql.nadel.instrumentation.parameters;


import graphql.PublicApi;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.normalized.ExecutableNormalizedOperation;
import graphql.schema.GraphQLSchema;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Parameters sent to {@link graphql.nadel.instrumentation.NadelInstrumentation} methods
 */
@SuppressWarnings("TypeParameterUnusedInFormals")
@PublicApi
public class NadelInstrumentationExecuteOperationParameters {
    private final InstrumentationState instrumentationState;
    @Nullable
    private final NormalizedQueryFromAst normalizedQueryFromAst;
    @Nullable
    private final ExecutableNormalizedOperation normalizedOperation;
    private final Document document;
    private final GraphQLSchema graphQLSchema;
    private final Map<String, Object> variables;
    private final OperationDefinition operationDefinition;
    private final Object context;

    public NadelInstrumentationExecuteOperationParameters(
            NormalizedQueryFromAst normalizedQueryFromAst,
            Document document,
            GraphQLSchema graphQLSchema,
            Map<String, Object> variables,
            OperationDefinition operationDefinition,
            InstrumentationState instrumentationState,
            Object context
    ) {
        this(normalizedQueryFromAst, null, document, graphQLSchema, variables, operationDefinition, instrumentationState, context);
    }

    public NadelInstrumentationExecuteOperationParameters(
            ExecutableNormalizedOperation normalizedOperation,
            Document document,
            GraphQLSchema graphQLSchema,
            Map<String, Object> variables,
            OperationDefinition operationDefinition,
            InstrumentationState instrumentationState,
            Object context
    ) {
        this(null, normalizedOperation, document, graphQLSchema, variables, operationDefinition, instrumentationState, context);
    }

    private NadelInstrumentationExecuteOperationParameters(
            @Nullable NormalizedQueryFromAst normalizedQueryFromAst,
            @Nullable ExecutableNormalizedOperation normalizedOperation,
            Document document,
            GraphQLSchema graphQLSchema,
            Map<String, Object> variables,
            OperationDefinition operationDefinition,
            InstrumentationState instrumentationState,
            Object context
    ) {
        this.instrumentationState = instrumentationState;
        this.normalizedOperation = normalizedOperation;
        this.normalizedQueryFromAst = normalizedQueryFromAst;
        this.document = document;
        this.graphQLSchema = graphQLSchema;
        this.variables = variables;
        this.operationDefinition = operationDefinition;
        this.context = context;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     * @return a new parameters object with the new state
     */
    public NadelInstrumentationExecuteOperationParameters withNewState(InstrumentationState instrumentationState) {
        return new NadelInstrumentationExecuteOperationParameters(normalizedQueryFromAst, normalizedOperation, document, graphQLSchema, variables, operationDefinition, instrumentationState, context);
    }

    @Nullable
    public NormalizedQueryFromAst getNormalizedQueryFromAst() {
        return normalizedQueryFromAst;
    }

    @Nullable
    public ExecutableNormalizedOperation getNormalizedOperation() {
        return normalizedOperation;
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        //noinspection unchecked
        return (T) instrumentationState;
    }

    public Document getDocument() {
        return document;
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public OperationDefinition getOperationDefinition() {
        return operationDefinition;
    }

    public Object getContext() {
        return context;
    }
}
