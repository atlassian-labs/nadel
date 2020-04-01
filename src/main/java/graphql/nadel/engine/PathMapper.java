package graphql.nadel.engine;

import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.nadel.util.FpKit;

import java.util.List;

public class PathMapper {

    public ExecutionPath mapPath(ExecutionPath executionPath, MergedField mergedField, UnapplyEnvironment environment) {
        List<Object> fieldSegments = patchLastFieldName(executionPath, mergedField);

        ExecutionStepInfo parentExecutionStepInfo = environment.parentExecutionStepInfo;
        ExecutionPath parentPath = parentExecutionStepInfo.getPath();
        if (environment.isHydrationTransformation) {
            //
            // Normally the parent path is all ok and hence there is nothing to add
            // but if we have a hydrated a field then we need to "merge" the paths not just append them
            // so for example
            //
            // /issue/reporter might lead to /userById and hence we need to collapse the top level hydrated field INTO the target field
            fieldSegments.remove(0);
            if (environment.batched) {
                fieldSegments.remove(0);
            }
            fieldSegments = FpKit.concat(parentPath.toList(), fieldSegments);
        }
        return ExecutionPath.fromList(fieldSegments);
    }

    private List<Object> patchLastFieldName(ExecutionPath executionPath, MergedField mergedField) {
        String fieldName = mergedField.getResultKey();
        List<Object> fieldSegments = executionPath.toList();
        for (int i = fieldSegments.size() - 1; i >= 0; i--) {
            Object segment = fieldSegments.get(i);
            if (segment instanceof String) {
                fieldSegments.set(i, fieldName);
                break;
            }
        }
        return fieldSegments;
    }
}
