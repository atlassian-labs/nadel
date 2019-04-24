package graphql.nadel.util;

import graphql.execution.ExecutionPath;

import java.util.List;

public class ExecutionPathUtils {

    /**
     * Returns true if the path ends with an integer segment eg /a/b[0]
     *
     * @param path the path to check
     *
     * @return true if its a list ending path
     */
    @SuppressWarnings("RedundantIfStatement")
    public static boolean isListEndingPath(ExecutionPath path) {
        List<Object> segments = path.toList();
        if (segments.isEmpty()) {
            return false;
        }
        if (segments.get(segments.size() - 1) instanceof String) {
            return false;
        }
        return true;
    }

    public static ExecutionPath removeLastSegment(ExecutionPath path) {
        List<Object> segments = path.toList();
        if (segments.isEmpty()) {
            return path;
        }
        segments = segments.subList(0, segments.size() - 1);
        return ExecutionPath.fromList(segments);
    }
}
