package graphql.nadel.engine.execution;

import graphql.Internal;
import graphql.introspection.Introspection;
import graphql.nadel.engine.result.ExecutionResultNode;

@Internal
public class ResolvedValueMapper {

    private static final String UNDERSCORE_TYPENAME = Introspection.TypeNameMetaFieldDef.getName();

    public ExecutionResultNode mapCompletedValue(ExecutionResultNode node, UnapplyEnvironment environment) {
        if (node.getFieldName().equals(UNDERSCORE_TYPENAME)) {
            String type = (String) node.getCompletedValue();
            String renamed = environment.typeRenameMappings.get(type);
            if (renamed != null) {
                return node.withNewCompletedValue(renamed);
            }
        }

        return node;
    }
}
