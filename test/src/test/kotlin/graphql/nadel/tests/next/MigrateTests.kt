package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.readValue
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.Scalars.GraphQLBoolean
import graphql.Scalars.GraphQLFloat
import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.execution.DataFetcherResult
import graphql.execution.RawVariables
import graphql.introspection.Introspection
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.engine.util.unwrapOne
import graphql.nadel.engine.util.whenType
import graphql.nadel.schema.NeverWiringFactory
import graphql.nadel.tests.ServiceCall
import graphql.nadel.tests.TestFixture
import graphql.nadel.tests.yamlObjectMapper
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.ScalarInfo
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import java.io.File

const val specificTest = ""

fun main() {
    val testSrc = File("/Users/fwang/Documents/Atlassian/nadel/test/src/test/kotlin")
    val fixtures = File("/Users/fwang/Documents/Atlassian/nadel/test/src/test/resources/fixtures")

    fixtures
        .walkTopDown()
        .filter {
            it.extension == "yaml" || it.extension == "yml"
        }
        .filterNot {
            it.name == "schema-transformation-is-applied.yml"
                || it.name == "dynamic-service-resolution-directive-not-in-interface.yml"
        }
        .filterNot {
            // Literally impossible to auto convert
            it.name == "one-synthetic-hydration-call-with-longer-path-and-same-named-overall-field.yml"
                || it.name == "one-hydration-call-with-longer-path-and-same-named-overall-field.yml"
        }
        .filter {
            specificTest.isBlank() || it.name == specificTest
        }
        .onEach {
            val file = it.relativeTo(fixtures).path
            println("Porting $file")
        }
        .forEach { fixtureFile ->
            val fixtureSubpackage = fixtureFile.parentFile.relativeTo(fixtures)
                .path
                .trim('/')
                .replace("/", ".")

            val packageName = "graphql.nadel.tests.legacy.$fixtureSubpackage"
            val testFixture = yamlObjectMapper.readValue<TestFixture>(fixtureFile.inputStream())
            val typeSpec = makeTestTypeSpec(packageName, testFixture)

            if (typeSpec != null) {
                val testClassName = ClassName(packageName, typeSpec.name!!)
                FileSpec.builder(testClassName)
                    .addType(typeSpec)
                    .build()
                    .writeTo(testSrc)

                writeTestSnapshotClass(
                    testClassName,
                    captured = makeTestExecutionCapture(testFixture),
                    sourceRoot = testSrc,
                )
            }
        }
}

fun makeTestExecutionCapture(fixture: TestFixture): TestExecutionCapture {
    val capture = TestExecutionCapture()

    fun toExecutionResult(result: JsonMap?): ExecutionResult {
        return object : ExecutionResult {
            override fun getErrors(): MutableList<GraphQLError> {
                return (result?.get("errors") as List<JsonMap>?)?.mapTo(mutableListOf(), ::toGraphQLError)
                    ?: mutableListOf()
            }

            override fun <T : Any?> getData(): T {
                return result?.get("data") as T
            }

            override fun isDataPresent(): Boolean {
                return result?.contains("data") == true
            }

            override fun getExtensions(): MutableMap<Any, Any>? {
                return result?.get("extensions") as MutableMap<Any, Any>?
            }

            override fun toSpecification(): MutableMap<String, Any?> {
                return (result as MutableMap<String, Any?>? ?: mutableMapOf())
                    .also {
                        it.remove("extensions")
                        if (errors.isEmpty()) {
                            it.remove("errors")
                        }
                    }
            }
        }
    }

    fixture.serviceCalls.forEach {
        capture.capture(
            service = it.serviceName,
            query = it.request.query,
            variables = it.request.variables,
            result = toExecutionResult(it.response)
        )
    }

    capture.capture(toExecutionResult(fixture.response))

    return capture
}

data class MigrateTestsContext(
    val packageName: String,
    /**
     * Service -> Typename -> TypeSpec
     */
    val typeSpecs: Map<String, Map<String, TypeSpec>>,
)

fun makeTestTypeSpec(
    packageName: String,
    fixture: TestFixture,
): TypeSpec? {
    if (!fixture.enabled || fixture.ignored) {
        return null
    }

    val simpleName = fixture.name
        .filter {
            it.isLetter() || it.isWhitespace()
        }
        .trim()

    val testClassName = ClassName(packageName, simpleName)
    val typeSpecs = createServiceTypes(testClassName, fixture)
    val context = MigrateTestsContext(packageName, typeSpecs)

    return with(context) {
        createTestClass(testClassName, fixture)
            .toBuilder()
            .also { builder ->
                typeSpecs.values
                    .asSequence()
                    .flatMap { it.values }
                    .forEach(builder::addType)
            }
            .build()
    }
}

fun createServiceTypes(
    testClassName: ClassName,
    fixture: TestFixture,
): Map<String, Map<String, TypeSpec>> {
    return fixture.underlyingSchema
        .entries
        .associate { (serviceName, schemaText) ->
            val schema = SchemaGenerator()
                .makeExecutableSchema(
                    SchemaParser()
                        .parse(schemaText),
                    RuntimeWiring.newRuntimeWiring()
                        .wiringFactory(NeverWiringFactory())
                        .build(),
                )

            serviceName to schema
                .typeMap
                .values
                .asSequence()
                .filterNot {
                    Introspection.isIntrospectionTypes(it.name)
                        || it == schema.queryType
                        || it == schema.mutationType
                        || it == schema.subscriptionType
                }
                .mapNotNull { type ->
                    constructType(
                        parentClassName = testClassName,
                        service = serviceName,
                        schema = schema,
                        type = type,
                    )?.let { type.name to it }
                }
                .toMap()
        }
}

context(MigrateTestsContext)
fun createTestClass(className: ClassName, fixture: TestFixture): TypeSpec {
    val services = fixture.overallSchema.keys
        .map { service ->
            makeService(fixture, service)
        }
        .joinToCode(separator = ", ")

    val variablesLiteral = if (fixture.variables.isEmpty()) {
        CodeBlock.Builder()
            .add("emptyMap()")
            .build()
    } else {
        javaValueToCodeLiteral(fixture.variables)
    }

    return TypeSpec.classBuilder(className)
        .superclass(ClassName("graphql.nadel.tests.legacy", "NadelLegacyIntegrationTest"))
        .addSuperclassConstructorParameter("query = %S", fixture.query)
        .addSuperclassConstructorParameter("variables = %L", variablesLiteral)
        .addSuperclassConstructorParameter("services = listOf(%L)", services)
        .build()
}

context(MigrateTestsContext)
fun makeService(fixture: TestFixture, serviceName: String): CodeBlock {
    val overallSchemaText = fixture.overallSchema[serviceName]
    val underlyingSchemaText = fixture.underlyingSchema[serviceName]

    val underlyingSchema = SchemaGenerator()
        .makeExecutableSchema(
            SchemaParser()
                .parse(underlyingSchemaText),
            RuntimeWiring.newRuntimeWiring()
                .wiringFactory(NeverWiringFactory())
                .build(),
        )

    val calls = fixture.serviceCalls
        .filter {
            it.serviceName == serviceName
        }

    val runtimeWiring = createRuntimeWiring(serviceName, underlyingSchema, calls)

    return CodeBlock.Builder()
        .addStatement(
            "Service(name=%S, overallSchema=%S, underlyingSchema=%S, runtimeWiring = %L)",
            serviceName,
            overallSchemaText,
            underlyingSchemaText,
            runtimeWiring,
        )
        .build()
}

context(MigrateTestsContext)
private fun createRuntimeWiring(
    serviceName: String,
    underlyingSchema: GraphQLSchema,
    calls: List<ServiceCall>,
): CodeBlock {
    data class DataFetcherReturns(
        val field: ExecutableNormalizedField,
        val result: Any?,
        val errors: List<Any?>?,
    )

    val dataFetcherReturnsByContainerTypeName = calls
        .flatMap { call ->
            val operation = createExecutableNormalizedOperationWithRawVariables(
                underlyingSchema,
                call.request.document,
                call.request.operationName,
                RawVariables.of(call.request.variables),
            )

            val isNamespaceChild = fun(child: ExecutableNormalizedField): Boolean {
                return child.objectTypeNames.size == 1 && child.normalizedArguments.isNotEmpty()
            }

            val errors = call.response?.get("errors") as List<Any?>?

            operation.topLevelFields
                .flatMap { topLevelField ->
                    val topLevelResult = (call.response?.get("data") as? JsonMap)?.get(topLevelField.resultKey)
                    if (topLevelField.normalizedArguments.isEmpty()) {
                        if (topLevelField.children.any(isNamespaceChild)) {
                            topLevelField.children
                                .map { child ->
                                    DataFetcherReturns(
                                        field = child,
                                        result = (topLevelResult as JsonMap?)?.get(child.resultKey),
                                        errors = errors,
                                    )
                                }
                        } else {
                            listOf(
                                DataFetcherReturns(
                                    field = topLevelField,
                                    result = topLevelResult,
                                    errors = errors,
                                )
                            )
                        }
                    } else {
                        listOf(
                            DataFetcherReturns(
                                field = topLevelField,
                                result = topLevelResult,
                                errors = errors,
                            ),
                        )
                    }
                }
        }
        .groupBy {
            it.field.objectTypeNames.single()
        }

    val namespacedDataFetchersBlock = dataFetcherReturnsByContainerTypeName
        .asSequence()
        .filter { (_, dataFetcherReturns) ->
            dataFetcherReturns.first().field.parent != null
        }
        .map { (containerType, dataFetcherReturns) ->
            val parent = dataFetcherReturns.first().field.parent
            val parentParentObjectTypeName = parent.objectTypeNames.single()

            val dataFetcher = CodeBlock.Builder()
                .beginControlFlow("type.dataFetcher(%S)", parent.name)
                .add("%T", Unit::class)
                .endControlFlow()
                .build()

            CodeBlock.Builder()
                .beginControlFlow("wiring.type(%S) { type ->", parentParentObjectTypeName)
                .add(dataFetcher)
                .endControlFlow()
                .build()
        }
        .joinToCode(separator = "\n")

    val toGraphQLError = MemberName("graphql.nadel.engine.util", "toGraphQLError")

    fun getResultObject(result: DataFetcherReturns): CodeBlock {
        val data = getResultObject(
            serviceName = serviceName,
            underlyingSchema = underlyingSchema,
            parent = result.field,
            children = result.field.children,
            result = result.result
        )

        return if (result.errors.isNullOrEmpty()) {
            data
        } else {
            val errors = CodeBlock.Builder()
                .add(
                    "listOf(%L)",
                    result.errors
                        .map {
                            CodeBlock.Builder()
                                .add(
                                    "%M(%L)",
                                    toGraphQLError,
                                    javaValueToCodeLiteral(it, explicitEmptyGenerics = true),
                                )
                                .build()
                        }
                        .joinToCode(separator = ", ")
                )
                .build()

            CodeBlock.Builder()
                .add("%T.newResult<%T>()", DataFetcherResult::class, Any::class)
                .add(".data(%L)", data)
                .add(".errors(%L)", errors)
                .add(".build()", data)
                .build()
        }
    }

    val dataFetchersBlock = dataFetcherReturnsByContainerTypeName
        .asSequence()
        .map { (containerType, dataFetcherReturns) ->
            val typeWiring = dataFetcherReturns
                .groupBy { it.field.name }
                .map { (fieldName, dataFetcherReturns) ->
                    val dataFetcher = if (dataFetcherReturns.isNotEmpty()) {
                        if (dataFetcherReturns.size == 1 && dataFetcherReturns.single().field.resolvedArguments.isEmpty()) {
                            val dataFetcherReturn = dataFetcherReturns.single()

                            getResultObject(dataFetcherReturn)
                        } else {
                            dataFetcherReturns
                                .map { dataFetcherReturn ->
                                    val result = getResultObject(dataFetcherReturn)

                                    val condition = getFieldCondition(dataFetcherReturn.field)

                                    CodeBlock.Builder()
                                        .beginControlFlow("if (%L)", condition)
                                        .add(result)
                                        .endControlFlow()
                                        .build()
                                }
                                .joinToCode(separator = "else ") // Keep space to ensure it doesn't merge with if keyword
                                .toBuilder()
                                .beginControlFlow("else")
                                .add("%L", null)
                                .endControlFlow()
                                .build()
                        }
                    } else {
                        CodeBlock.Builder()
                            .add("%L", null)
                            .build()
                    }

                    CodeBlock.Builder()
                        .beginControlFlow(".dataFetcher(%S) { env ->", fieldName)
                        .add(dataFetcher)
                        .endControlFlow()
                        .build()
                }
                .joinToCode(
                    prefix = "type",
                    separator = "\n",
                )

            CodeBlock.Builder()
                .beginControlFlow("wiring.type(%S) { type ->", containerType)
                .add(typeWiring)
                .endControlFlow()
                .build()
        }
        .joinToCode(separator = "\n")

    val typeResolversBlock = underlyingSchema.typeMap.values
        .asSequence()
        .filter {
            it is GraphQLInterfaceType || it is GraphQLUnionType
        }
        .map { abstractType ->
            val typeResolver = CodeBlock.Builder()
                .add("val obj = typeResolver.getObject<Any>()\n")
                .add("val typeName = obj.javaClass.simpleName.substringAfter(%S)\n", "_")
                .add("typeResolver.schema.getTypeAs(typeName)\n")
                .build()

            val typeWiring = CodeBlock.Builder()
                .beginControlFlow("type.typeResolver { typeResolver ->")
                .add(typeResolver)
                .endControlFlow()
                .build()

            CodeBlock.Builder()
                .beginControlFlow("wiring.type(%S) { type ->", abstractType.name)
                .add(typeWiring)
                .endControlFlow()
                .build()
        }
        .joinToCode(separator = "\n")

    val scalarBlock = underlyingSchema.typeMap.values
        .asSequence()
        .filterIsInstance<GraphQLScalarType>()
        .mapNotNull {
            when (it.name) {
                ExtendedScalars.Json.name -> CodeBlock.Builder()
                    .add("wiring.scalar(%T.Json)", ExtendedScalars::class)
                    .build()
                else -> {
                    if (ScalarInfo.isGraphqlSpecifiedScalar(it)) {
                        null
                    } else {
                        val realScalar = when (it.name) {
                            else -> "Json"
                        }

                        CodeBlock.Builder()
                            .add(
                                "wiring.scalar(%T.Builder().name(%S).aliasedScalar(%T.%L).build())",
                                AliasedScalar::class,
                                it.name,
                                ExtendedScalars::class,
                                realScalar,
                            )
                            .build()
                    }
                }
            }
        }
        .joinToCode(separator = "\n")

    return CodeBlock.Builder()
        .beginControlFlow("{ wiring ->")
        .add(namespacedDataFetchersBlock)
        .add(dataFetchersBlock)
        .add(typeResolversBlock)
        .add(scalarBlock)
        .endControlFlow()
        .build()
}

private fun Sequence<CodeBlock>.joinToCode(
    separator: String = ", ",
    prefix: String = "",
    suffix: String = "",
): CodeBlock {
    val builder = CodeBlock.Builder()
    builder.add(prefix)
    forEachIndexed { index, codeBlock ->
        if (index > 0) {
            builder.add(separator)
        }
        builder.add(codeBlock)
    }
    builder.add(suffix)
    return builder.build()
}

context(MigrateTestsContext)
private fun getResultObject(
    serviceName: String,
    underlyingSchema: GraphQLSchema,
    parent: ExecutableNormalizedField,
    children: List<ExecutableNormalizedField>,
    result: Any?,
): CodeBlock {
    if (result == null || children.isEmpty()) {
        return javaValueToCodeLiteral(result)
    }

    if (result is List<*>) {
        val listElements = result
            .map {
                getResultObject(
                    serviceName = serviceName,
                    underlyingSchema = underlyingSchema,
                    parent = parent,
                    children = children,
                    result = it,
                )
            }
            .joinToCode(separator = ", ")

        return CodeBlock.Builder()
            .add("listOf(%L)", listElements)
            .build()
    }

    val jsonObject = @Suppress("UNCHECKED_CAST") (result as JsonMap)
    val objectTypeName: String = inferTypename(children, jsonObject)
    val objectTypeSpec = typeSpecs[serviceName]!![objectTypeName]!!

    val params = children
        .asSequence()
        .filterNot {
            it.name == Introspection.TypeNameMetaFieldDef.name
        }
        .groupBy {
            it.name
        }
        .asSequence()
        .mapNotNull { (k, fields) ->
            fields
                .filter { field ->
                    field.objectTypeNames.contains(objectTypeName)
                }
                .takeIf {
                    it.isNotEmpty()
                }
                ?.let {
                    k to it
                }
        }
        .map { (_, fields) ->
            val result = if (fields.size > 1 && fields.any(ExecutableNormalizedField::hasChildren)) {
                fields.fold(null as JsonMap?) { acc, field ->
                    val map = (jsonObject[field.resultKey] as JsonMap?)
                    if (map == null) {
                        acc
                    } else {
                        (acc ?: emptyMap()) + map
                    }
                }
            } else {
                jsonObject[fields.first().resultKey]
            }

            val value = getResultObject(
                serviceName = serviceName,
                underlyingSchema = underlyingSchema,
                parent = fields.first(),
                children = fields.flatMap { it.children },
                result = result,
            )

            CodeBlock.Builder()
                .add("%N = %L", fields.first().name, value)
                .build()
        }
        .joinToCode(separator = ", ")

    return CodeBlock.Builder()
        .add("%N(%L)", objectTypeSpec, params)
        .build()
}

context(MigrateTestsContext)
private fun inferTypename(
    children: List<ExecutableNormalizedField>,
    jsonObject: JsonMap,
): String {
    // Directly specified by __typename
    return children
        .asSequence()
        .filter {
            it.fieldName == Introspection.TypeNameMetaFieldDef.name
        }
        .mapNotNull {
            jsonObject[it.resultKey] as String?
        }
        .singleOrNull()
        ?: children
            .flatMap { field ->
                field.objectTypeNames
                    .map { objectTypeName ->
                        objectTypeName to field
                    }
            }
            .groupBy(
                keySelector = { (objectTypeName) ->
                    objectTypeName
                },
                valueTransform = { (_, listOfFields) ->
                    listOfFields
                },
            )
            .entries
            .groupBy(
                keySelector = { (_, fields) ->
                    fields.mapTo(mutableSetOf()) { it.resultKey }
                },
                valueTransform = { (objectTypeName) ->
                    objectTypeName
                },
            )
            .asSequence()
            .mapNotNull { (resultKeySet, objectTypeNames) ->
                objectTypeNames.singleOrNull()?.let { it to resultKeySet }
            }
            .filter { (_, resultKeySet) ->
                jsonObject.keys == resultKeySet
            }
            .map { (objectType) ->
                objectType
            }
            .singleOrNull()
        ?: jsonObject[Introspection.TypeNameMetaFieldDef.name] as String
}

context(MigrateTestsContext)
private fun getFieldCondition(field: ExecutableNormalizedField): CodeBlock {
    if (field.resolvedArguments.isEmpty()) {
        return CodeBlock.Builder()
            .add("env.field.resultKey == %S", field.resultKey + "")
            .build()
    }

    return field.resolvedArguments
        .map { (name, value) ->
            CodeBlock.Builder()
                .add(
                    "env.getArgument<%T?>(%S) == %L",
                    Any::class,
                    name,
                    javaValueToCodeLiteral(value, explicitEmptyGenerics = true),
                )
                .build()
        }
        .joinToCode(separator = " && ")
}

context(MigrateTestsContext)
private fun javaValueToCodeLiteral(
    value: Any?,
    explicitEmptyGenerics: Boolean = false,
): CodeBlock {
    return when (value) {
        is Map<*, *> -> {
            if (value.isEmpty()) {
                if (explicitEmptyGenerics) {
                    val anything = Any::class.asTypeName().copy(nullable = true)
                    CodeBlock.Builder()
                        .add("emptyMap<%T, %T>()", anything, anything)
                        .build()
                } else {
                    CodeBlock.Builder()
                        .add("emptyMap()")
                        .build()
                }
            } else {
                val params = value
                    .map { (key, value) ->
                        CodeBlock.Builder()
                            .add("%S to %L", key, javaValueToCodeLiteral(value, explicitEmptyGenerics = true))
                            .build()
                    }
                    .joinToCode(separator = ", ")

                CodeBlock.Builder()
                    .add("mapOf(%L)", params)
                    .build()
            }
        }
        is List<*> -> {
            if (value.isEmpty()) {
                if (explicitEmptyGenerics) {
                    val anything = Any::class.asTypeName().copy(nullable = true)
                    CodeBlock.Builder()
                        .add("emptyList<%T>()", anything)
                        .build()
                } else {
                    CodeBlock.Builder()
                        .add("emptyList()")
                        .build()
                }
            } else {
                val params = value
                    .map {
                        javaValueToCodeLiteral(it, explicitEmptyGenerics = true)
                    }
                    .joinToCode(separator = ", ")

                CodeBlock.Builder()
                    .add("listOf(%L)", params)
                    .build()
            }
        }
        is String -> {
            CodeBlock.Builder()
                .add("%S", value)
                .build()
        }
        is Boolean, is Number -> {
            CodeBlock.Builder()
                .add("%L", value)
                .build()
        }
        else -> {
            CodeBlock.Builder()
                .add("%L", null)
                .build()
        }
    }
}

private fun needsOverride(type: GraphQLImplementingType, field: GraphQLFieldDefinition): Boolean {
    return type
        .interfaces
        .any { superType ->
            (superType as GraphQLInterfaceType).getField(field.name) != null
        }
}

private fun PropertySpec.Builder.overrideIfAbstract(
    type: GraphQLImplementingType,
    field: GraphQLFieldDefinition,
): PropertySpec.Builder {
    if (needsOverride(type, field)) {
        addModifiers(KModifier.OVERRIDE)
    }
    return this
}

private fun constructType(
    parentClassName: ClassName,
    service: String,
    schema: GraphQLSchema,
    type: GraphQLNamedType,
): TypeSpec? {
    val classPrefix = service.replaceFirstChar(Char::uppercase) + '_'

    fun getTypename(type: GraphQLType): TypeName {
        return type.whenType(
            listType = {
                val inner = it.unwrapOne()
                List::class.asTypeName()
                    .parameterizedBy(getTypename(inner))
                    .copy(nullable = true) // Nullable by default, unless ! overrides this
            },
            nonNull = {
                val inner = it.unwrapOne()
                getTypename(inner).copy(nullable = false)
            },
            unmodifiedType = {
                when (it.name) {
                    GraphQLInt.name -> Int::class.asTypeName()
                    GraphQLFloat.name -> Double::class.asTypeName()
                    GraphQLString.name -> String::class.asTypeName()
                    GraphQLBoolean.name -> Boolean::class.asTypeName()
                    GraphQLID.name -> String::class.asTypeName()
                    ExtendedScalars.Json.name -> Any::class.asTypeName()
                    ExtendedScalars.DateTime.name -> String::class.asTypeName()
                    ExtendedScalars.GraphQLLong.name -> Long::class.asTypeName()
                    ExtendedScalars.Url.name, "URL" -> String::class.asTypeName()
                    else -> {
                        parentClassName.nestedClass(classPrefix + it.name)
                    }
                }.copy(nullable = true) // Nullable by default, unless ! overrides this
            },
        )
    }

    return type.whenType(
        enumType = { enumType ->
            val type = TypeSpec.enumBuilder(classPrefix + type.name)
                .addModifiers(KModifier.PRIVATE)
            enumType.values.forEach { value ->
                type.addEnumConstant(value.name)
            }
            type.build()
        },
        inputObjectType = { inputObjectType ->
            TypeSpec.classBuilder(classPrefix + type.name)
                .addModifiers(KModifier.PRIVATE, KModifier.DATA)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameters(
                            inputObjectType.fields
                                .map { field ->
                                    ParameterSpec.builder(field.name, getTypename(field.type).copy(nullable = true))
                                        .defaultValue("%L", null)
                                        .build()
                                }
                        )
                        .build()
                )
                .addProperties(
                    inputObjectType.fields
                        .map { field ->
                            PropertySpec.builder(field.name, getTypename(field.type).copy(nullable = true))
                                .initializer(field.name)
                                .build()
                        }
                )
                .build()
        },
        interfaceType = { interfaceType ->
            TypeSpec.interfaceBuilder(classPrefix + type.name)
                .addModifiers(KModifier.PRIVATE)
                .addProperties(
                    interfaceType.fields
                        .map { field ->
                            PropertySpec.builder(field.name, getTypename(field.type).copy(nullable = true))
                                .overrideIfAbstract(interfaceType, field)
                                .build()
                        }
                )
                .addSuperinterfaces(
                    interfaceType.interfaces
                        .map {
                            getTypename(it).copy(nullable = false)
                        }
                )
                .build()
        },
        objectType = { objectType ->
            TypeSpec.classBuilder(classPrefix + type.name)
                .addModifiers(KModifier.PRIVATE, KModifier.DATA)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameters(
                            objectType.fields
                                .map { field ->
                                    ParameterSpec.builder(field.name, getTypename(field.type).copy(nullable = true))
                                        .defaultValue("%L", null)
                                        .build()
                                }
                        )
                        .build()
                )
                .addProperties(
                    objectType.fields
                        .map { field ->
                            PropertySpec.builder(field.name, getTypename(field.type).copy(nullable = true))
                                .overrideIfAbstract(objectType, field)
                                .initializer(field.name)
                                .build()
                        }
                )
                .addSuperinterfaces(
                    objectType.interfaces
                        .map {
                            getTypename(it).copy(nullable = false)
                        }
                )
                .addSuperinterfaces(
                    schema
                        .typeMap
                        .values
                        .asSequence()
                        .filterIsInstance<GraphQLUnionType>()
                        .filter { union ->
                            union.types.contains(type)
                        }
                        .map { union ->
                            getTypename(union).copy(nullable = false)
                        }
                        .toList()
                )
                .build()
        },
        scalarType = {
            null
        },
        unionType = { unionType ->
            TypeSpec.interfaceBuilder(classPrefix + unionType.name)
                .addModifiers(KModifier.PRIVATE, KModifier.SEALED)
                .build()
        },
    )
}
