package graphql.nadel.util

// todo: make internal once we merge api/ and engine-nextgen
object OperationNameUtil {
    /**
     * This is deprecated because we are moving away from this nadel_2_service naming scheme. We are
     * just forwarding operation names in the future. But for now, we need to support it for migration
     * purposes.
     */
    @Deprecated("")
    fun getLegacyOperationName(serviceName: String, originalOperationName: String?): String? {
        val baseName = "nadel_2_$serviceName"
        return if (originalOperationName != null) {
            baseName + "_" + originalOperationName
        } else {
            baseName
        }
    }
}
