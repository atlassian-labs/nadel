package graphql.nadel.dsl.builders;

import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.parser.antlr.StitchingDSLParser;

import java.util.stream.Collectors;

public class StitchingDslBuilder extends Builder<StitchingDSLParser.StitchingDSLContext, StitchingDsl> {
    @Override
    public StitchingDsl build(StitchingDSLParser.StitchingDSLContext input) {
        ServiceDefinitionBuilder svcBuilder = new ServiceDefinitionBuilder();

        return new StitchingDsl(svcBuilder.buildMany(input.serviceDefinition()));
    }
}
