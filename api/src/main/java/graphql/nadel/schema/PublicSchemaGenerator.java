package graphql.nadel.schema;

import graphql.Internal;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLDirectiveContainer;
import graphql.schema.GraphQLSchema;
import graphql.schema.transform.FieldVisibilitySchemaTransformation;
import graphql.schema.transform.VisibleFieldPredicate;

import static graphql.nadel.schema.NadelDirectives.HIDDEN_DIRECTIVE_DEFINITION;

@Internal
public class PublicSchemaGenerator {

    private static final VisibleFieldPredicate visibleFieldPredicate = environment -> {
        if (environment.getSchemaElement() instanceof GraphQLDirectiveContainer) {
            GraphQLDirectiveContainer container = (GraphQLDirectiveContainer) environment.getSchemaElement();
            return container.getDirectives()
                    .stream()
                    .map(GraphQLDirective::getName)
                    .noneMatch(HIDDEN_DIRECTIVE_DEFINITION.getName()::equalsIgnoreCase);
        }
        return true;
    };

    public static GraphQLSchema generatePublicSchema(GraphQLSchema privateSchema) {
        return new FieldVisibilitySchemaTransformation(visibleFieldPredicate).apply(privateSchema);
    }

}
