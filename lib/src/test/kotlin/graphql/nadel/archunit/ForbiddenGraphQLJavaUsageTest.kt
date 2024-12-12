package graphql.nadel.archunit

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaAccess
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ForbiddenGraphQLJavaUsageTest {
    @Test
    fun `do not access slow GraphQL Java methods`() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("graphql.nadel")

        val rule = noClasses()
            .should()
            .accessTargetWhere(
                object : DescribedPredicate<JavaAccess<*>>("access slow GraphQL Java methods") {
                    override fun test(t: JavaAccess<*>): Boolean {
                        // These methods sort the entire List alphabetically before returning it, not useful
                        return t.target.fullName.startsWith("graphql.schema.GraphQLSchema.getAllTypesAsList")
                            || t.target.fullName.startsWith("graphql.schema.GraphQLSchema.getAllElementsAsList")
                    }
                }
            )

        rule.check(importedClasses)
    }
}
