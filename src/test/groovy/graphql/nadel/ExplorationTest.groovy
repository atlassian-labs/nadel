package graphql.nadel

import graphql.language.ObjectTypeDefinition
import graphql.nadel.dsl.FieldTransformation
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.dsl.builders.StitchingDslBuilder
import graphql.nadel.parser.antlr.StitchingDSLLexer
import graphql.nadel.parser.antlr.StitchingDSLParser
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.ParserRuleContext;
import spock.lang.Specification

class ExplorationTest extends Specification {

    def mappers = [
            ServiceDefinitionContext : { i -> new ServiceDefinition(i.name().getText()) },
    ]

    def levelSkips = [
            "StitchingDSLContext" : [ single: false ]
    ]

    def applyMappers(ParserRuleContext input) {
        def key = input.getClass().getSimpleName()

        if (mappers[key]) {
            return mappers[key](input);
        }

        if (input.childCount >0) {
            if (levelSkips[key]) {
                if (levelSkips[key].single) {
                    return applyMappers(input.children[0]);
                }
                else {
                    // todo: how do I modify that lambda to make it shorter and more idiomatic?
                    return input.children.stream().map({c -> applyMappers(c)}).toArray();
                }
            }
        }
        return null;
    }

    def "simple service definition"() {
        given:
        def input = """
        service Foo {
            type Query {
                hello: String
            }
        }
       """
        /*
    public StitchingDsl parseDSL(String input) {
        StitchingDSLLexer lexer = new StitchingDSLLexer(new ANTLRInputStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StitchingDSLParser parser = new StitchingDSLParser(tokens);
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());

        StitchingDSLParser.StitchingDSLContext stitchingDSL = parser.stitchingDSL();

        NadelAntlrToLanguage antlrToLanguage = new NadelAntlrToLanguage(tokens);
        antlrToLanguage.visitStitchingDSL(stitchingDSL);

        Token stop = stitchingDSL.getStop();
        List<Token> allTokens = tokens.getTokens();
        if (stop != null && allTokens != null && !allTokens.isEmpty()) {
            Token last = allTokens.get(allTokens.size() - 1);
            //
            // do we have more tokens in the stream than we consumed in the parse?
            // if yes then its invalid.  We make sure its the same channel
            boolean notEOF = last.getType() != Token.EOF;
            boolean lastGreaterThanDocument = last.getTokenIndex() > stop.getTokenIndex();
            boolean sameChannel = last.getChannel() == stop.getChannel();
            if (notEOF && lastGreaterThanDocument && sameChannel) {
                throw new ParseCancellationException("There are more tokens in the query that have not been consumed");
            }
        }

        return antlrToLanguage.getStitchingDsl();
    }
        * */

        when:

        StitchingDSLLexer lexer = new StitchingDSLLexer(new ANTLRInputStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StitchingDSLParser parser = new StitchingDSLParser(tokens);
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());

        StitchingDSLParser.StitchingDSLContext stitchingDSL = parser.stitchingDSL();

        def res2 = new StitchingDslBuilder().build(stitchingDSL)

        println res2

        then:
        1 == 1
    }
}


