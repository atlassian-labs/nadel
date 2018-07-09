package graphql.nadel.dsl.builders;

import graphql.language.Directive;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.parser.antlr.StitchingDSLParser;

public class ServiceDefinitionBuilder extends Builder<StitchingDSLParser.ServiceDefinitionContext, ServiceDefinition> {
    @Override
    public ServiceDefinition build(StitchingDSLParser.ServiceDefinitionContext input) {
        DirectiveBuilder db = new DirectiveBuilder();
        return new ServiceDefinition(
                input.name().getText(),
                (Iterable<Directive>) null,
                null
        );
    }
}
