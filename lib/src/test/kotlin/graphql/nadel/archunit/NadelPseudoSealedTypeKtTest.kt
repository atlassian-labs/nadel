package graphql.nadel.archunit

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import graphql.language.DirectiveDefinition
import graphql.language.EnumTypeDefinition
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.SDLDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.SchemaDefinition
import graphql.language.SchemaExtensionDefinition
import graphql.language.Type
import graphql.language.TypeName
import graphql.language.UnionTypeDefinition
import graphql.language.UnionTypeExtensionDefinition
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedInputType
import graphql.schema.GraphQLNamedOutputType
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
        .importPackages("graphql.schema", "graphql.language")

    @Test
    fun `whenType(GraphQLFieldsContainer)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLFieldsContainer::class.java)
            .and()
            .areNotInterfaces()
            .equalsExactly(
                GraphQLInterfaceType::class,
                GraphQLObjectType::class,
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenType(GraphQLNamedInputType)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLNamedInputType::class.java)
            .and()
            .areNotInterfaces()
            .equalsExactly(
                GraphQLEnumType::class,
                GraphQLInputObjectType::class,
                GraphQLScalarType::class,
                // Should almost never be used though
                GraphQLTypeReference::class,
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenType(GraphQLNamedOutputType)`() {
        classes()
            .that()
            .areAssignableTo(GraphQLNamedOutputType::class.java)
            .and()
            .areNotInterfaces()
            .equalsExactly(
                GraphQLEnumType::class,
                GraphQLInterfaceType::class,
                GraphQLObjectType::class,
                GraphQLScalarType::class,
                GraphQLUnionType::class,
                // Should almost never be used though
                GraphQLTypeReference::class,
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
            .equalsExactly(
                GraphQLEnumType::class,
                GraphQLInputObjectType::class,
                GraphQLInterfaceType::class,
                GraphQLObjectType::class,
                GraphQLScalarType::class,
                GraphQLUnionType::class,
                // Should almost never be used though
                GraphQLTypeReference::class,
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
            .equalsExactly(
                GraphQLEnumType::class,
                GraphQLInterfaceType::class,
                GraphQLObjectType::class,
                GraphQLScalarType::class,
                GraphQLUnionType::class,
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
            .equalsExactly(
                GraphQLEnumType::class,
                GraphQLInputObjectType::class,
                GraphQLScalarType::class,
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenType(Type)`() {
        classes()
            .that()
            .areAssignableTo(Type::class.java)
            .and()
            .areNotInterfaces()
            .equalsExactly(
                ListType::class,
                NonNullType::class,
                TypeName::class,
            )
            .check(schemaClasses)
    }

    @Test
    fun `whenType(AnySDLDefinition)`() {
        classes()
            .that()
            .areAssignableTo(SDLDefinition::class.java)
            .and()
            .areNotInterfaces()
            .equalsExactly(
                DirectiveDefinition::class,
                EnumTypeDefinition::class,
                EnumTypeExtensionDefinition::class,
                InputObjectTypeDefinition::class,
                InputObjectTypeExtensionDefinition::class,
                InterfaceTypeDefinition::class,
                InterfaceTypeExtensionDefinition::class,
                ObjectTypeDefinition::class,
                ObjectTypeExtensionDefinition::class,
                ScalarTypeDefinition::class,
                ScalarTypeExtensionDefinition::class,
                SchemaDefinition::class,
                SchemaExtensionDefinition::class,
                UnionTypeDefinition::class,
                UnionTypeExtensionDefinition::class,
            )
            .check(schemaClasses)
    }
}
