package graphql.nadel.validation

import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.TypeName
import graphql.nadel.engine.util.unwrapOne
import graphql.nadel.util.splitBy
import graphql.nadel.validation.NadelTypeWrappingValidation.NullabilityToken.NOT_NULLABLE
import graphql.nadel.validation.NadelTypeWrappingValidation.NullabilityToken.NULLABLE
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelTypeWrappingValidation {
    enum class Rule {
        LHS_MUST_BE_LOOSER,
        RHS_MUST_BE_LOOSER,
        LHS_MUST_BE_LOOSER_OR_SAME,
        RHS_MUST_BE_LOOSER_OR_SAME,

        LHS_MUST_BE_STRICTER,
        RHS_MUST_BE_STRICTER,
        LHS_MUST_BE_STRICTER_OR_SAME,
        RHS_MUST_BE_STRICTER_OR_SAME,
    }

    fun isTypeWrappingValid(
        lhs: GraphQLType,
        rhs: GraphQLType,
        rule: Rule,
    ): Boolean {
        return isTypeWrappingValid(lhs, rhs) { lhsToken, rhsToken ->
            when (rule) {
                Rule.LHS_MUST_BE_LOOSER -> lhsToken.isLooserThan(rhsToken)
                Rule.LHS_MUST_BE_LOOSER_OR_SAME -> lhsToken.isLooserThanOrSame(rhsToken)
                Rule.RHS_MUST_BE_LOOSER -> rhsToken.isLooserThan(lhsToken)
                Rule.RHS_MUST_BE_LOOSER_OR_SAME -> rhsToken.isLooserThanOrSame(lhsToken)

                Rule.LHS_MUST_BE_STRICTER -> lhsToken.isStricterThan(rhsToken)
                Rule.LHS_MUST_BE_STRICTER_OR_SAME -> lhsToken.isStricterThanOrSame(rhsToken)
                Rule.RHS_MUST_BE_STRICTER -> rhsToken.isStricterThan(lhsToken)
                Rule.RHS_MUST_BE_STRICTER_OR_SAME -> rhsToken.isStricterThanOrSame(lhsToken)
            }
        }
    }

    private fun isTypeWrappingValid(
        lhs: GraphQLType,
        rhs: GraphQLType,
        nullableCheck: (lhsToken: NullabilityToken, rhsToken: NullabilityToken) -> Boolean,
    ): Boolean {
        if (lhs.countCardinality() != rhs.countCardinality()) {
            return false
        }

        val rhsTokens = rhs.getNullabilityTokens()
        val lhsTokens = lhs.getNullabilityTokens()
        // Cardinality is the same so the number of tokens is the same
        require(rhsTokens.size == lhsTokens.size)

        val areNullableChangesCompatible = lhs.getNullabilityTokens()
            .asSequence()
            .zip(rhs.getNullabilityTokens().asSequence())
            .all { (lhsToken, rhsToken) ->
                nullableCheck(lhsToken, rhsToken)
            }

        return areNullableChangesCompatible
    }

    private fun GraphQLType.countCardinality(): Int {
        return getTypeSequence()
            .count {
                it is GraphQLList
            }
    }

    /**
     * For each layer in a [GraphQLType] this will tell us whether it is nullable or not.
     *
     * Innermost layer is reported first, outermost layer is reported last.
     *
     * e.g.
     * given `[Test!]!` will output `[`[NOT_NULLABLE], [NOT_NULLABLE]`]`
     *
     * given `[Test!]` will output `[`[NOT_NULLABLE], [NULLABLE]`]`
     *
     * given `Test!` will output `[`[NOT_NULLABLE]`]`
     *
     * given `Test` will output `[`[NULLABLE]`]`
     *
     * etc.
     */
    private fun GraphQLType.getNullabilityTokens(): List<NullabilityToken> {
        return getTypeSequence()
            .filterNot {
                it is GraphQLUnmodifiedType
            }
            .onEach {
                require(it is GraphQLList || it is GraphQLNonNull)
            }
            .splitBy {
                it is GraphQLList
            }
            .map {
                if (it.isEmpty()) {
                    NULLABLE
                } else {
                    NOT_NULLABLE
                }
            }
            .toList()
            .asReversed()
    }

    /**
     * Constructs a [Sequence] which unwraps the entire [GraphQLType].
     *
     * i.e. given `[[Test]!]` it construct a sequence of [ListType], [NonNullType], [ListType], [TypeName]
     */
    private fun GraphQLType.getTypeSequence(): Sequence<GraphQLType> {
        return generateSequence(seed = this) { type ->
            type.unwrapOne()
                .takeUnless {
                    it === type
                }
        }
    }

    /**
     * Whether something is nullable or not.
     */
    private enum class NullabilityToken {
        NULLABLE,
        NOT_NULLABLE,
        ;

        fun isStricterThan(other: NullabilityToken): Boolean {
            return when (this) {
                NULLABLE -> false
                NOT_NULLABLE -> other == NULLABLE
            }
        }

        fun isLooserThan(other: NullabilityToken): Boolean {
            return when (this) {
                NULLABLE -> other == NOT_NULLABLE
                NOT_NULLABLE -> false
            }
        }

        fun isStricterThanOrSame(other: NullabilityToken): Boolean {
            return this == other || isStricterThan(other)
        }

        fun isLooserThanOrSame(other: NullabilityToken): Boolean {
            return this == other || isLooserThan(other)
        }
    }
}
