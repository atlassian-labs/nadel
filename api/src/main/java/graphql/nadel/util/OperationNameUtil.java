package graphql.nadel.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OperationNameUtil {
    private OperationNameUtil() {
    }

    /**
     * This is deprecated because we are moving away from this nadel_2_service naming scheme. We are
     * just forwarding operation names in the future. But for now, we need to support it for migration
     * purposes.
     */
    @Deprecated
    public static String getLegacyOperationName(@NotNull String serviceName, @Nullable String originalOperationName) {
        final var baseName = "nadel_2_" + serviceName;
        if (originalOperationName != null) {
            return baseName + "_" + originalOperationName;
        } else {
            return baseName;
        }
    }
}
