package graphql.nadel.util;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.language.SourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * A helper class that can to deal with graphql errors
 */
public class ErrorUtil {

    public static List<GraphQLError> createGraphQlErrorsFromRawErrors(List<Map<String, Object>> errors) {
        return errors.stream().map(ErrorUtil::createGraphqlErrorFromRawError).collect(toList());
    }

    public static GraphQLError createGraphqlErrorFromRawError(Map<String, Object> rawError) {

        GraphqlErrorBuilder errorBuilder = GraphqlErrorBuilder.newError();
        errorBuilder.message(String.valueOf(rawError.get("message")));
        errorBuilder.errorType(ErrorType.DataFetchingException);
        extractLocations(errorBuilder, rawError);
        extractPath(errorBuilder, rawError);
        extractExtensions(errorBuilder, rawError);
        return errorBuilder.build();
    }

    @SuppressWarnings("unchecked") // it needs to be this.  a class cast exception will tell it when its not
    private static void extractPath(GraphqlErrorBuilder errorBuilder, Map<String, Object> rawError) {
        List<Object> path = (List<Object>) rawError.get("path");
        if (path != null) {
            errorBuilder.path(path);
        }
    }

    @SuppressWarnings("unchecked") // it needs to be this.  a class cast exception will tell it when its not
    private static void extractExtensions(GraphqlErrorBuilder errorBuilder, Map<String, Object> rawError) {
        Map<String, Object> extensions = (Map<String, Object>) rawError.get("extensions");
        if (extensions != null) {
            errorBuilder.extensions(extensions);
        }
    }

    @SuppressWarnings("unchecked") // it needs to be this.  a class cast exception will tell it when its not
    private static void extractLocations(GraphqlErrorBuilder errorBuilder, Map<String, Object> rawError) {
        List locations = (List) rawError.get("locations");
        if (locations != null) {
            List<SourceLocation> sourceLocations = new ArrayList<>();
            for (Object locationObj : locations) {
                Map<String, Object> location = (Map<String, Object>) locationObj;
                Integer line = (Integer) location.get("line");
                Integer column = (Integer) location.get("column");
                sourceLocations.add(new SourceLocation(line, column));
            }
            errorBuilder.locations(sourceLocations);
        }
    }
}
