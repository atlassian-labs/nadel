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
    fun `does not use slow methods`() {
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
                                if (!invocation.targetOwner.getPackage().name.startsWith("graphql.nadel.definition")) {
                                    return false
                                }

                                val targetName = invocation.target.name
                                return targetName.startsWith("parse")
                                    || (targetName.startsWith("has") && targetName != "hashCode")
                                    || targetName.startsWith("from")
                            }
                        }
                    ),
                ),
            )

        rule.check(importedClasses)
    }
}
