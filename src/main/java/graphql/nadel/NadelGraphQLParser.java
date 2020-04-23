package graphql.nadel;

import graphql.language.NodeBuilder;
import graphql.nadel.dsl.NodeId;
import graphql.parser.GraphqlAntlrToLanguage;
import graphql.parser.MultiSourceReader;
import graphql.parser.Parser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Special Parser which adds an ID to every node
 */
public class NadelGraphQLParser extends Parser {

    @Override
    protected GraphqlAntlrToLanguage getAntlrToLanguage(CommonTokenStream tokens, MultiSourceReader multiSourceReader) {
        return new GraphqlAntlrToLanguage(tokens, multiSourceReader) {
            private int idCounter = 1;

            @Override
            protected void addCommonData(NodeBuilder nodeBuilder, ParserRuleContext parserRuleContext) {
                super.addCommonData(nodeBuilder, parserRuleContext);
                nodeBuilder.additionalData(additionalIdData());
            }

            private Map<String, String> additionalIdData() {
                Map<String, String> additionalData = new LinkedHashMap<>();
                String nodeIdVal = String.valueOf(idCounter++);
                additionalData.put(NodeId.ID, nodeIdVal);
                return additionalData;
            }
        };
    }
}
