package graphql.nadel.validation

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere
import com.tngtech.archunit.lang.conditions.ArchConditions.not
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import kotlin.test.Test

class NadelValidationDefinitionsTest {
    @Test
    fun `do not use parse definitions functions in validation`() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("graphql.nadel.validation")

        val rule = classes()
            .that(
                not(
                    JavaClass.Predicates.belongTo(
                        JavaClass.Predicates.simpleNameEndingWith("DefinitionParser")
                    ),
                ),
            )
            .should(
                not(
                    callMethodWhere(
                        object : DescribedPredicate<JavaMethodCall>("definition parse methods") {
                            override fun test(invocation: JavaMethodCall): Boolean {
                                return invocation.targetOwner.getPackage().name.startsWith("graphql.nadel.definition")
                                    && invocation.target.name.startsWith("parse")
                            }
                        }
                    ),
                ),
            )

        rule.check(importedClasses)
    }
}
