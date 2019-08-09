package graphql.nadel.hooks;

import graphql.execution.ExecutionStepInfo;
import graphql.language.Argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * A value object that contains a possible list of modified arguments and or variable values
 */
public class ModifiedArguments {

    private final List<Argument> fieldArgs;
    private final Map<String, Object> variables;

    private ModifiedArguments(Builder builder) {
        this.fieldArgs = unmodifiableList(new ArrayList<>(builder.fieldArgs));
        this.variables = unmodifiableMap(new LinkedHashMap<>(builder.variables));
    }

    public List<Argument> getFieldArgs() {
        return fieldArgs;
    }

    /**
     * NOTE : the variables returned here will be merged into the existing map of variables.   So you can
     * replace and add but you cannot delete.
     *
     * @return a new map of query variables to be merged in
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    public static Builder newModifiedArguments(ExecutionStepInfo executionStepInfo) {
        return new Builder(executionStepInfo);
    }

    public static class Builder {
        private List<Argument> fieldArgs;
        private Map<String, Object> variables;

        public Builder(ExecutionStepInfo executionStepInfo) {
            this.fieldArgs = assertNotNull(executionStepInfo).getField().getSingleField().getArguments();
            this.variables = Collections.emptyMap();
        }

        public Builder fieldArgs(List<Argument> fieldArgs) {
            this.fieldArgs = assertNotNull(fieldArgs);
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = assertNotNull(variables);
            return this;
        }

        public ModifiedArguments build() {
            return new ModifiedArguments(this);
        }
    }
}
