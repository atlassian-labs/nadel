package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.ExecutionPath;

import java.util.List;

@Internal
public class PathMapper {
    public ExecutionPath mapPath(ExecutionPath executionPath, String resultKey, UnapplyEnvironment environment) {
        List<Object> pathSegments = executionPath.toList();

        if (environment.isHydrationTransformation) {
            //
            // Normally the parent path is all ok and hence there is nothing to add
            // but if we have a hydrated a field then we need to "merge" the paths not just append them
            // so for example
            //
            // /issue/reporter might lead to /userById and hence we need to collapse the top level hydrated field INTO the target field
            List<Object> tmp = environment.parentNode.getExecutionPath().toList();
            tmp.add(pathSegments.get(pathSegments.size() - 1));
            pathSegments = tmp;
        } else {
            // This takes the parent path which is more correct and then appends the child path to it
            // e.g. given the parent path /devOpsRelationships/nodes[0] and
            //      given the path /relationships/nodes[0]
            //      where the field relationships was renamed to devOpsRelationships
            //      we should take the renamed parent path /devopsRelationships/nodes
            //      and append the child path [0] to it
            List<Object> tmp = environment.parentNode.getExecutionPath().toList();
            tmp.addAll(pathSegments.subList(tmp.size(), pathSegments.size()));
            pathSegments = tmp;
        }

        patchLastFieldName(pathSegments, resultKey);
        return ExecutionPath.fromList(pathSegments);
    }

    private void patchLastFieldName(List<Object> pathSegments, String resultKey) {
        for (int i = pathSegments.size() - 1; i >= 0; i--) {
            Object segment = pathSegments.get(i);
            if (segment instanceof String) {
                pathSegments.set(i, resultKey);
                break;
            }
        }
    }
}
