package graphql.nadel.hints;

import graphql.nadel.Service;

public interface NewDocumentCompiler {
    /**
     * Determines whether to use the compatability {@link graphql.nadel.compat.ExecutableNormalizedOperationToAstCompiler}.
     *
     * @param service the service in question
     * @return true to use new document printer, false to use previous version
     */
    boolean invoke(Service service);
}
