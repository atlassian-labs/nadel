package graphql.nadel;

import graphql.Internal;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.parser.antlr.StitchingDSLLexer;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.List;

@Internal
public class Parser {
    public StitchingDsl parseDSL(String input) {
        StitchingDSLLexer lexer = new StitchingDSLLexer(new ANTLRInputStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StitchingDSLParser parser = new StitchingDSLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new DefaultErrorStrategy());

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

    public static class ThrowingErrorListener extends BaseErrorListener {
        public static ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg, RecognitionException e)
        {

            String sourceName = recognizer.getInputStream().getSourceName();
            if (!sourceName.isEmpty()) {
                sourceName = String.format("%s:%d:%d: ", sourceName, line, charPositionInLine);
            }

            throw new ParseCancellationException(sourceName+"line "+line+":"+charPositionInLine+" "+msg);
        }
    }

}
