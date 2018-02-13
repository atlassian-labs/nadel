package graphql.nadel;

import graphql.Internal;
import graphql.nadel.parser.antlr.StitchingDSLLexer;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;

@Internal
public class Parser {


    public void parseDSL(String input) {
        StitchingDSLLexer lexer = new StitchingDSLLexer(new ANTLRInputStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StitchingDSLParser parser = new StitchingDSLParser(tokens);
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());
        StitchingDSLParser.StitchingDSLContext stitchingDSL = parser.stitchingDSL();
    }

}
