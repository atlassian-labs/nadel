package graphql.nadel.enginekt.transform.artificial

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.NormalizedField

/**
 * Artificial fields are fields that do not exist in the original query.
 *
 * These are fields added in by Nadel to gather more information from the
 * underlying service in order to perform hydration etc.
 *
 * This class helps generate fields that are correctly aliased to avoid
 * collisions with fields in the original query e.g. given
 *
 * ```graphql
 * type Cat implements Pet {
 *   name: String @renamed(from: ["tag", "name"])
 * }
 * ```
 *
 * and a query:
 *
 * ```
 * {
 *   pet {
 *     ... on Cat { name }
 *   }
 * }
 * ```
 *
 * then the query to the underlying service should look something similar to:
 *
 * ```
 * {
 *   pet {
 *     ... on Cat {
 *       my_alias__tag: tag { name }
 *     }
 *   }
 * }
 * ```
 */
class AliasHelper(private val alias: String) {
    val typeNameResultKey by lazy {
        TypeNameMetaFieldDef.name + "__" + alias
    }

    fun getResultKey(field: NormalizedField): String {
        return getResultKey(fieldName = field.name)
    }

    fun getResultKey(fieldName: String): String {
        return alias + "__" + fieldName
    }

    fun mapQueryPathRespectingResultKey(
        path: QueryPath,
    ): QueryPath {
        return path.mapIndexed { index, segment ->
            when (index) {
                0 -> getResultKey(segment)
                else -> segment
            }
        }
    }

    fun toArtificial(field: NormalizedField): NormalizedField {
        // The first field must be aliased as it is an artificial field
        return field.toBuilder()
            .alias(getResultKey(field.fieldName))
            .build()
    }
}
