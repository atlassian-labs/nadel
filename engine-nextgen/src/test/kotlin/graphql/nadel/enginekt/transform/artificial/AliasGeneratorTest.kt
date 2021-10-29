package graphql.nadel.enginekt.transform.artificial

import graphql.normalized.ExecutableNormalizedField
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isGreaterThan
import strikt.assertions.size

inline fun <T> listOf(size: Int, init: (index: Int) -> T): List<T> {
    val list = ArrayList<T>(size)
    for (i in 0 until size) {
        list.add(init(i))
    }
    return list
}

class AliasGeneratorTest : DescribeSpec({
    describe("generated aliases") {
        val field = mockk<ExecutableNormalizedField> {
            every { resultKey } returns "test_key"
        }
        val aliases = listOf(size = 1000) {
            AliasGenerator.getAlias(tag = "rename", field = field).also(::println)
        }

        it("has dynamic aspect") {
            expectThat(aliases)
                .get {
                    toSet()
                }
                .size
                .isGreaterThan(50)
        }

        it("conforms to valid alias name") {
            // See https://spec.graphql.org/draft/#sec-Field-Alias
            expectThat(aliases)
                .all {
                    assertThat("starts with letter or underscore") {
                        it.first().isLetter() || it.first() == '_'
                    }
                    assertThat("remaining characters are either letters, digits or underscores") {
                        it.drop(1).all { char ->
                            char.isLetterOrDigit() || char == '_'
                        }
                    }
                }
        }
    }
})
