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
import graphql.nadel.definition.hydration.hasHydratedDefinition
import graphql.nadel.definition.renamed.hasRenameDefinition
import graphql.nadel.definition.renamed.parseRenamedOrNull
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType
import kotlin.test.Test

class NadelValidationDefinitionsTest {
    /**
     * Validation should use [NadelInstructionDefinitionRegistry] instead.
     *
     * e.g. avoid [GraphQLFieldDefinition.hasHydratedDefinition] and use [NadelInstructionDefinitionRegistry.isHydrated]
     *
     * e.g. avoid [GraphQLDirectiveContainer.hasRenameDefinition] and use [NadelInstructionDefinitionRegistry.isRenamed]
     *
     * e.g. avoid [GraphQLNamedType.parseRenamedOrNull] and use [NadelInstructionDefinitionRegistry.getRenamedOrNull]
     */
    @Test
    fun `do not use raw instruction definition parse methods in validation`() {
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
