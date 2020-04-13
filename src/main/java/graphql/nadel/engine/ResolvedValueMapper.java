package graphql.nadel.engine;

import graphql.execution.nextgen.result.ResolvedValue;
import graphql.introspection.Introspection;
import graphql.nadel.result.ExecutionResultNode;

public class ResolvedValueMapper {

    private static final String UNDERSCORE_TYPENAME = Introspection.TypeNameMetaFieldDef.getName();

    public ResolvedValue mapResolvedValue(ExecutionResultNode node, UnapplyEnvironment environment) {
        if (node.getFieldName().equals(UNDERSCORE_TYPENAME)) {
            String type = (String) node.getResolvedValue().getCompletedValue();
            String renamed = environment.typeRenameMappings.get(type);
            if (renamed != null) {
                return node.getResolvedValue().transform(builder -> builder.completedValue(renamed).build());
            }
        }

        return node.getResolvedValue();
    }
}
