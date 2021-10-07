package graphql.nadel.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OperationNameUtil {
    private OperationNameUtil() {
    }

    @Deprecated
    public static String buildOperationName(@NotNull String serviceNabem, @Nullable String originalOperationName) {
        final var baseName = "nadel_2_" + serviceNabem;
        if (originalOperationName != null) {
            return baseName + "_" + originalOperationName;
        } else {
            return baseName;
        }
    }
}
