package graphql.nadel

import graphql.language.AstPrinter
import graphql.language.Document


class TestUtil {

    static String printAstCompact(Document document) {
        AstPrinter.printAst(document).replaceAll("\\s+", " ").trim()
    }
}
