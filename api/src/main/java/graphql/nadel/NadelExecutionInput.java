package graphql.nadel;

import graphql.PublicApi;
import graphql.execution.ExecutionId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;
import static graphql.GraphQLContext.newContext;
import static java.util.Objects.requireNonNull;

@PublicApi
public class NadelExecutionInput {

    @NotNull
    private final String query;
    @Nullable
    private final String operationName;
    @Nullable
    private final Object context;
    @NotNull
    private final Map<String, Object> variables;
    @Nullable
    private final String artificialFieldsUUID;
    @Nullable
    private final ExecutionId executionId;
    @NotNull
    private final NadelExecutionHints nadelExecutionHints;

    private NadelExecutionInput(
            String query,
            @Nullable String operationName,
            @Nullable Object context,
            Map<String, Object> variables,
            @Nullable String artificialFieldsUUID,
            @Nullable ExecutionId executionId,
            @NotNull NadelExecutionHints nadelExecutionHints
    ) {
        this.query = requireNonNull(query);
        this.operationName = operationName;
        this.context = context;
        this.variables = requireNonNull(variables);
        this.artificialFieldsUUID = artificialFieldsUUID;
        this.executionId = executionId;
        this.nadelExecutionHints = nadelExecutionHints;
    }

    public static Builder newNadelExecutionInput() {
        return new Builder();
    }

    @NotNull
    public String getQuery() {
        return query;
    }

    @Nullable
    public String getArtificialFieldsUUID() {
        return artificialFieldsUUID;
    }

    @Nullable
    public String getOperationName() {
        return operationName;
    }

    @Nullable
    public Object getContext() {
        return context;
    }

    @NotNull
    public Map<String, Object> getVariables() {
        return new LinkedHashMap<>(variables);
    }

    /**
     * @return Id that will be/was used to execute this operation.
     */
    @Nullable
    public ExecutionId getExecutionId() {
        return executionId;
    }

    @NotNull
    public NadelExecutionHints getNadelExecutionHints() {
        return nadelExecutionHints;
    }

    public static class Builder {
        private String query;
        private String operationName;
        private Object context = newContext().build();
        private Map<String, Object> variables = new LinkedHashMap<>();
        private String artificialFieldsUUID;
        private ExecutionId executionId;
        private NadelExecutionHints nadelExecutionHints = NadelExecutionHints.newHints().build();

        private Builder() {
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder operationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        public Builder context(Object context) {
            this.context = context;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public Builder executionId(ExecutionId executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder artificialFieldsUUID(String artificialFieldsUUID) {
            this.artificialFieldsUUID = artificialFieldsUUID;
            return this;
        }

        public Builder nadelExecutionHints(NadelExecutionHints nadelExecutionHints) {
            this.nadelExecutionHints = assertNotNull(nadelExecutionHints);
            return this;
        }

        public Builder transformExecutionHints(Consumer<NadelExecutionHints.Builder> builderConsumer) {
            final NadelExecutionHints.Builder hintsBuilder = this.nadelExecutionHints.toBuilder();

            builderConsumer.accept(hintsBuilder);

            this.nadelExecutionHints = hintsBuilder.build();

            return this;
        }

        public NadelExecutionInput build() {
            return new NadelExecutionInput(query, operationName, context, variables, artificialFieldsUUID, executionId, nadelExecutionHints);
        }
    }
}
