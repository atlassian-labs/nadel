package graphql.nadel.tests.next

import graphql.language.AstPrinter
import graphql.language.Directive
import graphql.language.DirectiveDefinition
import graphql.language.DirectivesContainer
import graphql.language.Document
import graphql.language.EnumTypeDefinition
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.FieldDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.InputValueDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.NamedNode
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.StringValue
import graphql.language.Type
import graphql.language.TypeDefinition
import graphql.language.UnionTypeDefinition
import graphql.language.UnionTypeExtensionDefinition
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.util.NadelBuiltInTypes
import graphql.parser.Parser
import graphql.schema.idl.TypeUtil

private data class UnderlyingSchemaGeneratorContext(
    val typeRenames: Map<String, String>,
)

/**
 * Does a best effort at generating an equivalent underlying schema for tests.
 *
 * This will not be helpful for ALL tests. Some tests may require you to do more modifications.
 */
fun makeUnderlyingSchema(overallSchema: String): String {
    val document = Parser.parse(overallSchema)
    val typeRenames =
        document
            .children
            .filterIsInstance<TypeDefinition<*>>()
            .filter {
                it.hasRenamed()
            }
            .associate {
                it.name to it.getRenamedFrom()
            }

    val context = UnderlyingSchemaGeneratorContext(typeRenames)

    return with(context) {
        make(document)
    }
}

private val fakeQueryType = Parser()
    .parseDocument(
        """
            type Query {
                echo: String
            }
        """.trimIndent(),
    )
    .definitions
    .single()

context(UnderlyingSchemaGeneratorContext)
private fun make(overallSchema: Document): String {
    return overallSchema
        .children
        .let { definitions ->
            val hasQueryType = definitions
                .any {
                    // Let's assume nobody is renaming the query type
                    it is ObjectTypeDefinition
                        && it !is ObjectTypeExtensionDefinition
                        && it.name == "Query"
                }
            val hasDefer = definitions
                .any {
                    it is DirectiveDefinition
                        && it.name == "defer"
                }

            definitions
                .let {
                    if (hasQueryType) it else (it + fakeQueryType)
                }
                .let {
                    if (hasDefer) it else (it + NadelDirectives.deferDirectiveDefinition)
                }
        }
        .asSequence()
        .map {
            // In theory the overall schema can extend types not in this schema… Let's leave that for another day
            when (val type = it) {
                is ObjectTypeExtensionDefinition -> transformObjectTypeExtensionDefinition(type)
                is InterfaceTypeExtensionDefinition -> transformInterfaceTypeExtensionDefinition(type)
                is UnionTypeExtensionDefinition -> transformUnionTypeExtensionDefinition(type)
                is InputObjectTypeExtensionDefinition -> transformInputObjectTypeExtensionDefinition(type)
                is ScalarTypeExtensionDefinition -> transformScalarTypeExtensionDefinition(type)
                is EnumTypeExtensionDefinition -> transformEnumTypeExtensionDefinition(type)
                is ObjectTypeDefinition -> transformObjectTypeDefinition(type)
                is InterfaceTypeDefinition -> transformInterfaceTypeDefinition(type)
                is UnionTypeDefinition -> transformUnionTypeDefinition(type)
                is InputObjectTypeDefinition -> transformInputObjectTypeDefinition(type)
                is ScalarTypeDefinition -> transformScalarTypeDefinition(type)
                is EnumTypeDefinition -> transformEnumTypeDefinition(type)
                is TypeDefinition<*> -> throw UnsupportedOperationException()
                else -> it
            }
        }
        .joinToString(separator = "\n\n") {
            AstPrinter.printAst(it)
        }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformObjectTypeExtensionDefinition(type: ObjectTypeExtensionDefinition): ObjectTypeExtensionDefinition {
    return type.transformExtension { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .fieldDefinitions(transformFields(type.fieldDefinitions))
            .implementz(type.getUnderlyingImplements())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformInterfaceTypeExtensionDefinition(type: InterfaceTypeExtensionDefinition): InterfaceTypeExtensionDefinition {
    return type.transformExtension { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .definitions(transformFields(type.fieldDefinitions))
            .implementz(type.getUnderlyingImplements())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformUnionTypeExtensionDefinition(type: UnionTypeExtensionDefinition): UnionTypeExtensionDefinition {
    return type.transformExtension { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .memberTypes(
                type.memberTypes
                    .map {
                        it.getUnderlyingType()
                    },
            )
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformInputObjectTypeExtensionDefinition(type: InputObjectTypeExtensionDefinition): InputObjectTypeExtensionDefinition {
    return type.transformExtension { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .inputValueDefinitions(
                type.inputValueDefinitions
                    .map { inputValue ->
                        inputValue.transform { inputValueBuilder ->
                            inputValueBuilder
                                .type(inputValue.type.getUnderlyingType())
                        }
                    }
            )
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformScalarTypeExtensionDefinition(type: ScalarTypeExtensionDefinition): ScalarTypeExtensionDefinition {
    return type.transformExtension { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformEnumTypeExtensionDefinition(type: EnumTypeExtensionDefinition): EnumTypeExtensionDefinition {
    return type.transformExtension { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformObjectTypeDefinition(type: ObjectTypeDefinition): ObjectTypeDefinition {
    return type.transform { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .fieldDefinitions(transformFields(type.fieldDefinitions))
            .implementz(type.getUnderlyingImplements())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformInterfaceTypeDefinition(type: InterfaceTypeDefinition): InterfaceTypeDefinition {
    return type.transform { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .definitions(transformFields(type.fieldDefinitions))
            .implementz(type.getUnderlyingImplements())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformUnionTypeDefinition(type: UnionTypeDefinition): UnionTypeDefinition {
    return type.transform { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .memberTypes(
                type.memberTypes
                    .map {
                        it.getUnderlyingType()
                    },
            )
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformInputObjectTypeDefinition(type: InputObjectTypeDefinition): InputObjectTypeDefinition {
    return type.transform { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
            .inputValueDefinitions(transformInputValueDefinitions(type.inputValueDefinitions))
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformScalarTypeDefinition(type: ScalarTypeDefinition): ScalarTypeDefinition {
    return type.transform { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformEnumTypeDefinition(type: EnumTypeDefinition): EnumTypeDefinition {
    return type.transform { typeBuilder ->
        typeBuilder
            .name(type.getUnderlyingName())
            .directives(type.directives.filterNotNadelDirectives())
    }
}

private fun DirectivesContainer<*>.hasRenamed(): Boolean {
    return hasDirective(NadelDirectives.renamedDirectiveDefinition.name)
}

private fun DirectivesContainer<*>.getRenamedFromOrNull(): String? {
    return getDirectives(NadelDirectives.renamedDirectiveDefinition.name)
        ?.emptyOrSingle()
        ?.getArgument("from")
        ?.value
        ?.let {
            (it as StringValue).value
        }
}

private fun DirectivesContainer<*>.getRenamedFrom(): String {
    return getRenamedFromOrNull()!!
}

context(UnderlyingSchemaGeneratorContext)
private fun DirectivesContainer<*>.getUnderlyingName(): String? {
    return getRenamedFromOrNull() ?: (this as NamedNode<*>).name
}

context(UnderlyingSchemaGeneratorContext)
private fun List<Directive>.filterNotNadelDirectives(): List<Directive> {
    return filterNot {
        it.name in NadelBuiltInTypes.builtInDirectiveSyntaxTypeNames
    }
}

context(UnderlyingSchemaGeneratorContext)
private fun Type<*>.getUnderlyingType(): Type<*> {
    val unwrappedTypeName = unwrapAll().name
    val renamedFrom = typeRenames[unwrappedTypeName]
    // Do nothing if no rename
        ?: return this

    val newTypeName = TypeUtil.simplePrint(this)
        .replace(
            unwrappedTypeName,
            renamedFrom,
        )

    return Parser()
        .parseDocument("type Query{e:$newTypeName}")
        .children
        .single()
        .let {
            // This is why the pay me the big bucks
            (it as ObjectTypeDefinition).fieldDefinitions.single().type
        }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformInputValueDefinitions(inputValueDefinitions: List<InputValueDefinition>): List<InputValueDefinition>? {
    return inputValueDefinitions
        .map { inputValue ->
            inputValue.transform { inputValueBuilder ->
                inputValueBuilder
                    .type(inputValue.type.getUnderlyingType())
            }
        }
}

context(UnderlyingSchemaGeneratorContext)
private fun transformFields(fieldDefinitions: List<FieldDefinition>): List<FieldDefinition> {
    return fieldDefinitions
        .asSequence()
        .filterNot { field ->
            field.hasDirective(NadelDirectives.hydratedDirectiveDefinition.name)
        }
        .map { field ->
            field.transform { fieldBuilder ->

                fieldBuilder
                    .name(field.getUnderlyingName())
                    .directives(field.directives.filterNotNadelDirectives())
                    .type(field.type.getUnderlyingType())
            }
        }
        .toList()
}

context(UnderlyingSchemaGeneratorContext)
private fun ImplementingTypeDefinition<*>.getUnderlyingImplements(): List<Type<*>> {
    return implements
        .map {
            it.getUnderlyingType()
        }
}

