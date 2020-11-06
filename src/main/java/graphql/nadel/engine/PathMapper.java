package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.ExecutionPath;

import java.util.List;

@Internal
public class PathMapper {

    public ExecutionPath mapPath(ExecutionPath executionPath, String resultKey, UnapplyEnvironment environment) {
        List<Object> fieldSegments = patchLastFieldName(executionPath, resultKey);

        List<Object> tmp = environment.parentNode.getExecutionPath().toList();

        // if we have a executionPath like /issues/reporterId[0] and a parentNode executionPath
        // like /issues/reporters[0] this replaces the first string field name
        if (fieldSegments.get(fieldSegments.size() - 1) instanceof Integer) {
            tmp.set(tmp.size() - 1, fieldSegments.get(fieldSegments.size() - 2));
        }

        //
        // If the case that we have a hydrated field,
        // or a renamed parent field and are about to hydrate a child field
        // we need to "merge" the paths not just append them
        // so for example:
        //
        // /issue/reporterId might lead to /userById and hence we need to collapse the top level hydrated field INTO the target field
        // /issue/reporterIds[0] might need to be changed to /issue/reporters[0]
        tmp.add(fieldSegments.get(fieldSegments.size() - 1));
        fieldSegments = tmp;

        return ExecutionPath.fromList(fieldSegments);
    }

    private List<Object> patchLastFieldName(ExecutionPath executionPath, String resultKey) {
        List<Object> fieldSegments = executionPath.toList();
        for (int i = fieldSegments.size() - 1; i >= 0; i--) {
            Object segment = fieldSegments.get(i);
            if (segment instanceof String) {
                fieldSegments.set(i, resultKey);
                break;
            }
        }
        return fieldSegments;
    }
}
