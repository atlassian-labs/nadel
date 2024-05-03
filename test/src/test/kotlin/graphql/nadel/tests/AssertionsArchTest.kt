package graphql.nadel.tests

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMember
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import graphql.Assert.assertTrue
import org.junit.jupiter.api.Test

class AssertionsArchTest {
    @Test
    fun `tests should use Kotlin assertTrue`() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.OnlyIncludeTests())
            .importPackages("graphql.nadel")

        classes()
            .that(excludeGrandfatheredClasses())
            .should()
            .onlyAccessMembersThat(
                object : DescribedPredicate<JavaMember>("is an assert function not from Kotlin test module") {
                    override fun test(member: JavaMember): Boolean {
                        return when (member) {
                            is JavaMethod -> {
                                when {
                                    member.name.contains("assert") -> {
                                        // Limit which assertions can be invoked
                                        member.owner.packageName.startsWith("kotlin.test")
                                            || member.owner.packageName.startsWith("graphql.nadel")
                                            || member.owner.packageName.startsWith("org.skyscreamer.jsonassert")
                                    }
                                    else -> true
                                }
                            }
                            else -> true
                        }
                    }
                },
            )
            .check(importedClasses)
    }

    private fun excludeGrandfatheredClasses() =
        object : DescribedPredicate<JavaClass>("exclude grandfathered classes") {
            private val grandfatheredClasses = setOf(
                "graphql.nadel.tests.JsonAssertionsKt",
                "graphql.nadel.tests.util.StriktUtilKt",
                "graphql.nadel.tests.hooks.exceptions-in-hydration-call-that-fail-with-errors-are-reflected-in-the-result",
                "graphql.nadel.tests.hooks.Exceptions_in_hydration_call_that_fail_with_errors_are_reflected_in_the_resultKt",
                "graphql.nadel.tests.hooks.can-delete-fields-and-types",
                "graphql.nadel.tests.hooks.chained-instrumentation-works-as-expected",
            )

            override fun test(t: JavaClass): Boolean {
                return t.fullName !in grandfatheredClasses
                    && grandfatheredClasses
                    .none {
                        // Not a subclass
                        @Suppress("ConvertToStringTemplate") // "$it$" looks weird lol
                        t.fullName.startsWith(it + "$")
                    }
            }
        }
}
