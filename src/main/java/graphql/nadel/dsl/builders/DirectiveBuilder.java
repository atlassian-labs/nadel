package graphql.nadel.dsl.builders;

import graphql.language.Directive;
import graphql.nadel.parser.antlr.StitchingDSLParser;

public class DirectiveBuilder extends Builder<StitchingDSLParser.DirectiveContext, Directive> {
    @Override
    public Directive build(StitchingDSLParser.DirectiveContext input) {
        // fixme: add support to arguments here
        return new Directive(input.name().getText());
    }
}
