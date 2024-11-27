package graphql.nadel.archunit

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeReference
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType
import kotlin.test.Test

/**
 * Tests that expected implementations of GraphQL Java classes have not changed.
 */
class NadelPseudoSealedTypeKtTest {
    private val schemaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("graphql.schema")

    @Test
    fun `whenType(GraphQLFieldsContainer)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLFieldsContainer::class.java)
            .and()
            .areNotInterfaces()
            .should(
                ArchUnitEqualsClassesExactly(
                    GraphQLInterfaceType::class,
                    GraphQLObjectType::class,
                ),
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenType(GraphQLNamedType)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLNamedType::class.java)
            .and()
            .areNotInterfaces()
            .should(
                ArchUnitEqualsClassesExactly(
                    GraphQLEnumType::class,
                    GraphQLInputObjectType::class,
                    GraphQLInterfaceType::class,
                    GraphQLObjectType::class,
                    GraphQLScalarType::class,
                    GraphQLUnionType::class,
                    // Should almost never be used though
                    GraphQLTypeReference::class,
                ),
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenUnmodifiedType(GraphQLOutputType)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLOutputType::class.java)
            .and()
            .areAssignableTo(GraphQLUnmodifiedType::class.java)
            .and()
            .areNotInterfaces()
            .should(
                ArchUnitEqualsClassesExactly(
                    GraphQLEnumType::class,
                    GraphQLInterfaceType::class,
                    GraphQLObjectType::class,
                    GraphQLScalarType::class,
                    GraphQLUnionType::class,
                ),
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenUnmodifiedType(GraphQLInputType)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLInputType::class.java)
            .and()
            .areAssignableTo(GraphQLUnmodifiedType::class.java)
            .and()
            .areNotInterfaces()
            .should(
                ArchUnitEqualsClassesExactly(
                    GraphQLEnumType::class,
                    GraphQLInputObjectType::class,
                    GraphQLScalarType::class,
                ),
            )
            .check(schemaClasses)
    }
}
