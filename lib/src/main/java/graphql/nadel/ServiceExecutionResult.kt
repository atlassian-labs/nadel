package graphql.nadel

import graphql.nadel.engine.util.MutableJsonMap

class ServiceExecutionResult private constructor(
    val data: MutableJsonMap,
    val errors: MutableList<out MutableJsonMap>,
    val extensions: MutableJsonMap,
) {
    fun copy(
        data: MutableJsonMap = this.data,
        errors: MutableList<out MutableJsonMap> = this.errors,
        extensions: MutableJsonMap = this.extensions,
    ): ServiceExecutionResult {
        return ServiceExecutionResult(data, errors, extensions)
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    companion object {
        /**
         * This "behaves" the same way as a constructor in Kotlin, but
         * you have to go through hoops to call this in Java.
         */
        @JvmName("_new")
        operator fun invoke(
            data: MutableJsonMap = LinkedHashMap(),
            errors: MutableList<out MutableJsonMap> = ArrayList(),
            extensions: MutableJsonMap = LinkedHashMap(),
        ) = ServiceExecutionResult(
            data = data,
            errors = errors,
            extensions = extensions,
        )
    }

    /**
     * This is mainly for Java consumers. To ensure they actually give us Mutable data structures.
     * And because Java doesn't support default arguments so [copy] doesn't really work there.
     */
    class Builder @JvmOverloads constructor(src: ServiceExecutionResult? = null) {
        private var data: MutableJsonMap? = src?.data
        private var errors: MutableList<out MutableJsonMap>? = src?.errors
        private var extensions: MutableJsonMap? = src?.extensions

        fun data(data: HashMap<String, Any?>?): Builder = apply { this.data = data }
        fun errors(errors: ArrayList<HashMap<String, Any?>>?): Builder = apply { this.errors = errors }
        fun extensions(extensions: HashMap<String, Any?>?): Builder = apply { this.extensions = extensions }

        fun build() = ServiceExecutionResult(
            data = data ?: LinkedHashMap(),
            errors = errors ?: ArrayList(),
            extensions = extensions ?: LinkedHashMap(),
        )
    }
}
