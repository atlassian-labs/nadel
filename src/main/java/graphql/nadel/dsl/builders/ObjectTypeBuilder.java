package graphql.nadel.dsl.builders;

import graphql.language.ObjectTypeDefinition;
import graphql.nadel.parser.antlr.StitchingDSLParser;

public class ObjectTypeBuilder extends Builder<StitchingDSLParser.ObjectTypeDefinitionContext, ObjectTypeDefinition> {
    @Override
    public ObjectTypeDefinition build(StitchingDSLParser.ObjectTypeDefinitionContext input) {
        return new ObjectTypeDefinition(
                input.name().getText(),
                null,
                null,
                null
        );
    }
}
