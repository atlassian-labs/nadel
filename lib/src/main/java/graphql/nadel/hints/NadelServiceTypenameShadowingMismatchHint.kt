package graphql.nadel.hints

import graphql.nadel.Service
import graphql.normalized.ExecutableNormalizedField

interface NadelServiceTypenameShadowingHint {
    fun isShadowingEnabled(): Boolean

    fun onMismatch(
        userContext: Any?,
        oldDecision: Boolean,
        newDecision: Boolean,
        service: Service,
        objectTypeName: String,
        field: ExecutableNormalizedField,
    )

    companion object {
        internal val default = object : NadelServiceTypenameShadowingHint {
            override fun isShadowingEnabled(): Boolean {
                return false
            }

            override fun onMismatch(
                userContext: Any?,
                oldDecision: Boolean,
                newDecision: Boolean,
                service: Service,
                objectTypeName: String,
                field: ExecutableNormalizedField,
            ) {
            }
        }
    }
}
