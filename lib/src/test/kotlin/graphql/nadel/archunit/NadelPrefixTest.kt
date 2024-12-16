package graphql.nadel.archunit

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

class NadelPrefixTest {
    @Test
    fun `classes must be prefixed with Nadel`() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("graphql.nadel")

        val rule = classes()
            .that()
            .areTopLevelClasses()
            .and()
            .areNotPrivate()
            .and(excludeGrandfatheredClassesFromNadelPrefix())
            .and().haveSimpleNameNotEndingWith("Util")
            .and().haveSimpleNameNotEndingWith("UtilKt")
            .should()
            .haveSimpleNameStartingWith("Nadel")

        rule.check(importedClasses)
    }

    @Test
    fun `inner classes should not be prefixed with Nadel`() {
        val importedClasses = ClassFileImporter().importPackages("graphql.nadel")

        val rule = classes()
            .that()
            .areNotTopLevelClasses()
            .should()
            .haveSimpleNameNotStartingWith("Nadel")

        rule.check(importedClasses)
    }

    private fun excludeGrandfatheredClassesFromNadelPrefix() =
        object : DescribedPredicate<JavaClass>("ignore grandfathered classes") {
            private val grandfatheredClasses = setOf(
                "graphql.nadel.NextgenEngine",
                "graphql.nadel.Service",
                "graphql.nadel.ServiceLike",
                "graphql.nadel.ServiceExecution",
                "graphql.nadel.ServiceExecutionFactory",
                "graphql.nadel.ServiceExecutionHydrationDetails",
                "graphql.nadel.ServiceExecutionParameters",
                "graphql.nadel.ServiceExecutionResult",
                "graphql.nadel.dsl.FieldMappingDefinition",
                "graphql.nadel.dsl.RemoteArgumentDefinition",
                "graphql.nadel.dsl.RemoteArgumentSource",
                "graphql.nadel.dsl.TypeMappingDefinition",
                "graphql.nadel.engine.blueprint.Factory",
                "graphql.nadel.engine.blueprint.IntrospectionService",
                "graphql.nadel.engine.blueprint.SharedTypesAnalysis",
                "graphql.nadel.engine.document.DocumentPredicates",
                "graphql.nadel.engine.transform.hydration.batch.BatchedArgumentValue",
                "graphql.nadel.engine.transform.query.DynamicServiceResolution",
                "graphql.nadel.engine.transform.result.NotMutableError",
                "graphql.nadel.engine.transform.result.json.IllegalNodeTypeException",
                "graphql.nadel.engine.transform.result.json.JsonNode",
                "graphql.nadel.engine.transform.result.json.JsonNodeExtractor",
                "graphql.nadel.engine.transform.result.json.JsonNodes",
                "graphql.nadel.engine.util.AliasesKt",
                "graphql.nadel.hints.AllDocumentVariablesHint",
                "graphql.nadel.hints.LegacyOperationNamesHint",
                "graphql.nadel.hints.NewBatchHydrationGroupingHint",
                "graphql.nadel.hints.NewResultMergerAndNamespacedTypename",
                "graphql.nadel.hooks.CreateServiceContextParams",
                "graphql.nadel.hooks.ServiceOrError",
                "graphql.nadel.instrumentation.ChainedNadelInstrumentation",
                "graphql.nadel.instrumentation.parameters.ErrorData",
                "graphql.nadel.instrumentation.parameters.ErrorType",
                "graphql.nadel.schema.NeverWiringFactory",
                "graphql.nadel.schema.OverallSchemaGenerator",
                "graphql.nadel.schema.QuerySchemaGenerator",
                "graphql.nadel.schema.SchemaTransformationHook",
                "graphql.nadel.schema.ServiceSchemaProblem",
                "graphql.nadel.schema.UnderlyingSchemaGenerator",
                "graphql.nadel.util.LogKit",
            )

            override fun test(t: JavaClass): Boolean {
                return t.fullName !in grandfatheredClasses
            }
        }
}
