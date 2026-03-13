// @formatter:off
package graphql.nadel.tests.next.fixtures.basic

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<StandardIntrospectionQueryTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StandardIntrospectionQueryTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": {
     *     "__schema": {
     *       "queryType": {
     *         "name": "Query"
     *       },
     *       "mutationType": null,
     *       "subscriptionType": null,
     *       "types": [
     *         {
     *           "kind": "SCALAR",
     *           "name": "Boolean",
     *           "description": "Built-in Boolean",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "SCALAR",
     *           "name": "Float",
     *           "description": "Built-in Float",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "SCALAR",
     *           "name": "ID",
     *           "description": "Built-in ID",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "SCALAR",
     *           "name": "Int",
     *           "description": "Built-in Int",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "SCALAR",
     *           "name": "JSON",
     *           "description": "A JSON scalar",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "INPUT_OBJECT",
     *           "name": "NadelBatchObjectIdentifiedBy",
     *           "description": "This is required by batch hydration to understand how to pull out
     * objects from the batched result",
     *           "isOneOf": false,
     *           "fields": null,
     *           "inputFields": [
     *             {
     *               "name": "sourceId",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "resultId",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "INPUT_OBJECT",
     *           "name": "NadelHydrationArgument",
     *           "description": "This allows you to hydrate new values into fields",
     *           "isOneOf": false,
     *           "fields": null,
     *           "inputFields": [
     *             {
     *               "name": "name",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "value",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "JSON",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "INPUT_OBJECT",
     *           "name": "NadelHydrationCondition",
     *           "description": "Specify a condition for the hydration to activate",
     *           "isOneOf": false,
     *           "fields": null,
     *           "inputFields": [
     *             {
     *               "name": "result",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "INPUT_OBJECT",
     *                   "name": "NadelHydrationResultCondition",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "INPUT_OBJECT",
     *           "name": "NadelHydrationResultCondition",
     *           "description": "Specify a condition for the hydration to activate based on the
     * result",
     *           "isOneOf": false,
     *           "fields": null,
     *           "inputFields": [
     *             {
     *               "name": "sourceField",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "predicate",
     *               "description": null,
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "INPUT_OBJECT",
     *                   "name": "NadelHydrationResultFieldPredicate",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "INPUT_OBJECT",
     *           "name": "NadelHydrationResultFieldPredicate",
     *           "description": null,
     *           "isOneOf": true,
     *           "fields": null,
     *           "inputFields": [
     *             {
     *               "name": "startsWith",
     *               "description": null,
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "equals",
     *               "description": null,
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "JSON",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "matches",
     *               "description": null,
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "Query",
     *           "description": null,
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "echo",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "SCALAR",
     *           "name": "String",
     *           "description": "Built-in String",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "__Directive",
     *           "description": null,
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "name",
     *               "description": "The __Directive type represents a Directive that a server
     * supports.",
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "description",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "isRepeatable",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Boolean",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "locations",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "ENUM",
     *                       "name": "__DirectiveLocation",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "args",
     *               "description": null,
     *               "args": [
     *                 {
     *                   "name": "includeDeprecated",
     *                   "description": null,
     *                   "type": {
     *                     "kind": "SCALAR",
     *                     "name": "Boolean",
     *                     "ofType": null
     *                   },
     *                   "defaultValue": "false",
     *                   "isDeprecated": false,
     *                   "deprecationReason": null
     *                 }
     *               ],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "OBJECT",
     *                       "name": "__InputValue",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "ENUM",
     *           "name": "__DirectiveLocation",
     *           "description": "An enum describing valid locations where a directive can be
     * placed",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": [
     *             {
     *               "name": "QUERY",
     *               "description": "Indicates the directive is valid on queries.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "MUTATION",
     *               "description": "Indicates the directive is valid on mutations.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "SUBSCRIPTION",
     *               "description": "Indicates the directive is valid on subscriptions.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "FIELD",
     *               "description": "Indicates the directive is valid on fields.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "FRAGMENT_DEFINITION",
     *               "description": "Indicates the directive is valid on fragment definitions.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "FRAGMENT_SPREAD",
     *               "description": "Indicates the directive is valid on fragment spreads.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "INLINE_FRAGMENT",
     *               "description": "Indicates the directive is valid on inline fragments.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "VARIABLE_DEFINITION",
     *               "description": "Indicates the directive is valid on variable definitions.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "SCHEMA",
     *               "description": "Indicates the directive is valid on a schema SDL definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "SCALAR",
     *               "description": "Indicates the directive is valid on a scalar SDL definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "OBJECT",
     *               "description": "Indicates the directive is valid on an object SDL definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "FIELD_DEFINITION",
     *               "description": "Indicates the directive is valid on a field SDL definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "ARGUMENT_DEFINITION",
     *               "description": "Indicates the directive is valid on a field argument SDL
     * definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "INTERFACE",
     *               "description": "Indicates the directive is valid on an interface SDL
     * definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "UNION",
     *               "description": "Indicates the directive is valid on an union SDL definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "ENUM",
     *               "description": "Indicates the directive is valid on an enum SDL definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "ENUM_VALUE",
     *               "description": "Indicates the directive is valid on an enum value SDL
     * definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "INPUT_OBJECT",
     *               "description": "Indicates the directive is valid on an input object SDL
     * definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "INPUT_FIELD_DEFINITION",
     *               "description": "Indicates the directive is valid on an input object field SDL
     * definition.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "__EnumValue",
     *           "description": null,
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "name",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "description",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "isDeprecated",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Boolean",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "deprecationReason",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "__Field",
     *           "description": null,
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "name",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "description",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "args",
     *               "description": null,
     *               "args": [
     *                 {
     *                   "name": "includeDeprecated",
     *                   "description": null,
     *                   "type": {
     *                     "kind": "SCALAR",
     *                     "name": "Boolean",
     *                     "ofType": null
     *                   },
     *                   "defaultValue": "false",
     *                   "isDeprecated": false,
     *                   "deprecationReason": null
     *                 }
     *               ],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "OBJECT",
     *                       "name": "__InputValue",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "type",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "OBJECT",
     *                   "name": "__Type",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "isDeprecated",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Boolean",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "deprecationReason",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "__InputValue",
     *           "description": null,
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "name",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "description",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "type",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "OBJECT",
     *                   "name": "__Type",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "defaultValue",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "isDeprecated",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "Boolean",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "deprecationReason",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "__Schema",
     *           "description": "A GraphQL Introspection defines the capabilities of a GraphQL
     * server. It exposes all available types and directives on the server, the entry points for query,
     * mutation, and subscription operations.",
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "description",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "types",
     *               "description": "A list of all types supported by this server.",
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "OBJECT",
     *                       "name": "__Type",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "queryType",
     *               "description": "The type that query operations will be rooted at.",
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "OBJECT",
     *                   "name": "__Type",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "mutationType",
     *               "description": "If this server supports mutation, the type that mutation
     * operations will be rooted at.",
     *               "args": [],
     *               "type": {
     *                 "kind": "OBJECT",
     *                 "name": "__Type",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "directives",
     *               "description": "A list of all directives supported by this server.",
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "OBJECT",
     *                       "name": "__Directive",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "subscriptionType",
     *               "description": "If this server support subscription, the type that subscription
     * operations will be rooted at.",
     *               "args": [],
     *               "type": {
     *                 "kind": "OBJECT",
     *                 "name": "__Type",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "OBJECT",
     *           "name": "__Type",
     *           "description": null,
     *           "isOneOf": null,
     *           "fields": [
     *             {
     *               "name": "kind",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "ENUM",
     *                   "name": "__TypeKind",
     *                   "ofType": null
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "name",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "description",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "fields",
     *               "description": null,
     *               "args": [
     *                 {
     *                   "name": "includeDeprecated",
     *                   "description": null,
     *                   "type": {
     *                     "kind": "SCALAR",
     *                     "name": "Boolean",
     *                     "ofType": null
     *                   },
     *                   "defaultValue": "false",
     *                   "isDeprecated": false,
     *                   "deprecationReason": null
     *                 }
     *               ],
     *               "type": {
     *                 "kind": "LIST",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "NON_NULL",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "OBJECT",
     *                     "name": "__Field",
     *                     "ofType": null
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "interfaces",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "LIST",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "NON_NULL",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "OBJECT",
     *                     "name": "__Type",
     *                     "ofType": null
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "possibleTypes",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "LIST",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "NON_NULL",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "OBJECT",
     *                     "name": "__Type",
     *                     "ofType": null
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "enumValues",
     *               "description": null,
     *               "args": [
     *                 {
     *                   "name": "includeDeprecated",
     *                   "description": null,
     *                   "type": {
     *                     "kind": "SCALAR",
     *                     "name": "Boolean",
     *                     "ofType": null
     *                   },
     *                   "defaultValue": "false",
     *                   "isDeprecated": false,
     *                   "deprecationReason": null
     *                 }
     *               ],
     *               "type": {
     *                 "kind": "LIST",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "NON_NULL",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "OBJECT",
     *                     "name": "__EnumValue",
     *                     "ofType": null
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "inputFields",
     *               "description": null,
     *               "args": [
     *                 {
     *                   "name": "includeDeprecated",
     *                   "description": null,
     *                   "type": {
     *                     "kind": "SCALAR",
     *                     "name": "Boolean",
     *                     "ofType": null
     *                   },
     *                   "defaultValue": "false",
     *                   "isDeprecated": false,
     *                   "deprecationReason": null
     *                 }
     *               ],
     *               "type": {
     *                 "kind": "LIST",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "NON_NULL",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "OBJECT",
     *                     "name": "__InputValue",
     *                     "ofType": null
     *                   }
     *                 }
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "ofType",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "OBJECT",
     *                 "name": "__Type",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "isOneOf",
     *               "description": "This field is considered experimental because it has not yet
     * been ratified in the graphql specification",
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "Boolean",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "specifiedByURL",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "specifiedByUrl",
     *               "description": null,
     *               "args": [],
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "isDeprecated": true,
     *               "deprecationReason": "This legacy name has been replaced by `specifiedByURL`"
     *             }
     *           ],
     *           "inputFields": null,
     *           "interfaces": [],
     *           "enumValues": null,
     *           "possibleTypes": null
     *         },
     *         {
     *           "kind": "ENUM",
     *           "name": "__TypeKind",
     *           "description": "An enum describing what kind of type a given __Type is",
     *           "isOneOf": null,
     *           "fields": null,
     *           "inputFields": null,
     *           "interfaces": null,
     *           "enumValues": [
     *             {
     *               "name": "SCALAR",
     *               "description": "Indicates this type is a scalar. `specifiedByURL` is a valid
     * field",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "OBJECT",
     *               "description": "Indicates this type is an object. `fields` and `interfaces` are
     * valid fields.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "INTERFACE",
     *               "description": "Indicates this type is an interface. `fields` and
     * `possibleTypes` are valid fields.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "UNION",
     *               "description": "Indicates this type is a union. `possibleTypes` is a valid
     * field.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "ENUM",
     *               "description": "Indicates this type is an enum. `enumValues` is a valid
     * field.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "INPUT_OBJECT",
     *               "description": "Indicates this type is an input object. `inputFields` is a
     * valid field.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "LIST",
     *               "description": "Indicates this type is a list. `ofType` is a valid field.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "NON_NULL",
     *               "description": "Indicates this type is a non-null. `ofType` is a valid field.",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "possibleTypes": null
     *         }
     *       ],
     *       "directives": [
     *         {
     *           "name": "include",
     *           "description": "Directs the executor to include this field or fragment only when
     * the `if` argument is true",
     *           "locations": [
     *             "FIELD",
     *             "FRAGMENT_SPREAD",
     *             "INLINE_FRAGMENT"
     *           ],
     *           "args": [
     *             {
     *               "name": "if",
     *               "description": "Included when true.",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Boolean",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "skip",
     *           "description": "Directs the executor to skip this field or fragment when the `if`
     * argument is true.",
     *           "locations": [
     *             "FIELD",
     *             "FRAGMENT_SPREAD",
     *             "INLINE_FRAGMENT"
     *           ],
     *           "args": [
     *             {
     *               "name": "if",
     *               "description": "Skipped when true.",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Boolean",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "hydrated",
     *           "description": "This allows you to hydrate new values into fields",
     *           "locations": [
     *             "FIELD_DEFINITION"
     *           ],
     *           "args": [
     *             {
     *               "name": "service",
     *               "description": "The target service",
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "field",
     *               "description": "The target top level field",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "identifiedBy",
     *               "description": "How to identify matching results",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "\"id\"",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "inputIdentifiedBy",
     *               "description": "How to identify matching results",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "INPUT_OBJECT",
     *                       "name": "NadelBatchObjectIdentifiedBy",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "defaultValue": "[]",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "indexed",
     *               "description": "Are results indexed",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Boolean",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "false",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "batchSize",
     *               "description": "The batch size",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Int",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "200",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "timeout",
     *               "description": "The timeout to use when completing hydration",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Int",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "-1",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "arguments",
     *               "description": "The arguments to the hydrated field",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "INPUT_OBJECT",
     *                       "name": "NadelHydrationArgument",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "when",
     *               "description": "Specify a condition for the hydration to activate",
     *               "type": {
     *                 "kind": "INPUT_OBJECT",
     *                 "name": "NadelHydrationCondition",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": true
     *         },
     *         {
     *           "name": "virtualType",
     *           "description": null,
     *           "locations": [
     *             "OBJECT"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "defaultHydration",
     *           "description": "This allows you to hydrate new values into fields",
     *           "locations": [
     *             "OBJECT",
     *             "INTERFACE"
     *           ],
     *           "args": [
     *             {
     *               "name": "field",
     *               "description": "The backing level field for the data",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "idArgument",
     *               "description": "Name of the ID argument on the backing field",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "identifiedBy",
     *               "description": "How to identify matching results",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "\"id\"",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "batchSize",
     *               "description": "The batch size",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Int",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "200",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "timeout",
     *               "description": "The timeout to use when completing hydration",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "Int",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "-1",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "idHydrated",
     *           "description": "This allows you to hydrate new values into fields",
     *           "locations": [
     *             "FIELD_DEFINITION"
     *           ],
     *           "args": [
     *             {
     *               "name": "idField",
     *               "description": "The field that holds the ID value(s) to hydrate",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "identifiedBy",
     *               "description": "(Optional override) how to identify matching results",
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "defaultValue": "null",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "stubbed",
     *           "description": "Allows you to introduce stubbed fields or types.\n\nStubbed fields
     * or fields that return stubbed types _always_ return null.\n\nStubbed fields are meant to allow
     * frontend clients consume new schema elements earlier so that they can iterate faster.",
     *           "locations": [
     *             "OBJECT",
     *             "FIELD_DEFINITION",
     *             "UNION",
     *             "ENUM",
     *             "INPUT_OBJECT"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "renamed",
     *           "description": "This allows you to rename a type or field in the overall schema",
     *           "locations": [
     *             "SCALAR",
     *             "OBJECT",
     *             "FIELD_DEFINITION",
     *             "INTERFACE",
     *             "UNION",
     *             "ENUM",
     *             "INPUT_OBJECT"
     *           ],
     *           "args": [
     *             {
     *               "name": "from",
     *               "description": "The type to be renamed",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "hidden",
     *           "description": "Indicates that the field is not available for queries or
     * introspection",
     *           "locations": [
     *             "FIELD_DEFINITION"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "hydrationRemainingArguments",
     *           "description": null,
     *           "locations": [
     *             "ARGUMENT_DEFINITION"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "defer",
     *           "description": null,
     *           "locations": [
     *             "FRAGMENT_SPREAD",
     *             "INLINE_FRAGMENT"
     *           ],
     *           "args": [
     *             {
     *               "name": "if",
     *               "description": null,
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "Boolean",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             },
     *             {
     *               "name": "label",
     *               "description": null,
     *               "type": {
     *                 "kind": "SCALAR",
     *                 "name": "String",
     *                 "ofType": null
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "namespaced",
     *           "description": "Indicates that the field is a namespaced field.",
     *           "locations": [
     *             "FIELD_DEFINITION"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "partition",
     *           "description": "This allows you to partition a field",
     *           "locations": [
     *             "FIELD_DEFINITION"
     *           ],
     *           "args": [
     *             {
     *               "name": "pathToPartitionArg",
     *               "description": "The path to the split point",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "LIST",
     *                   "name": null,
     *                   "ofType": {
     *                     "kind": "NON_NULL",
     *                     "name": null,
     *                     "ofType": {
     *                       "kind": "SCALAR",
     *                       "name": "String",
     *                       "ofType": null
     *                     }
     *                   }
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "deprecated",
     *           "description": "Marks the field, argument, input field or enum value as
     * deprecated",
     *           "locations": [
     *             "FIELD_DEFINITION",
     *             "ARGUMENT_DEFINITION",
     *             "ENUM_VALUE",
     *             "INPUT_FIELD_DEFINITION"
     *           ],
     *           "args": [
     *             {
     *               "name": "reason",
     *               "description": "The reason for the deprecation",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": "\"No longer supported\"",
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "specifiedBy",
     *           "description": "Exposes a URL that specifies the behaviour of this scalar.",
     *           "locations": [
     *             "SCALAR"
     *           ],
     *           "args": [
     *             {
     *               "name": "url",
     *               "description": "The URL that specifies the behaviour of this scalar.",
     *               "type": {
     *                 "kind": "NON_NULL",
     *                 "name": null,
     *                 "ofType": {
     *                   "kind": "SCALAR",
     *                   "name": "String",
     *                   "ofType": null
     *                 }
     *               },
     *               "defaultValue": null,
     *               "isDeprecated": false,
     *               "deprecationReason": null
     *             }
     *           ],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "oneOf",
     *           "description": "Indicates an Input Object is a OneOf Input Object.",
     *           "locations": [
     *             "INPUT_OBJECT"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         },
     *         {
     *           "name": "experimental_disableErrorPropagation",
     *           "description": "This directive disables error propagation when a non nullable field
     * returns null for the given operation.",
     *           "locations": [
     *             "QUERY",
     *             "MUTATION",
     *             "SUBSCRIPTION"
     *           ],
     *           "args": [],
     *           "isRepeatable": false
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "__schema": {
            |       "queryType": {
            |         "name": "Query"
            |       },
            |       "mutationType": null,
            |       "subscriptionType": null,
            |       "types": [
            |         {
            |           "kind": "SCALAR",
            |           "name": "Boolean",
            |           "description": "Built-in Boolean",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "SCALAR",
            |           "name": "Float",
            |           "description": "Built-in Float",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "SCALAR",
            |           "name": "ID",
            |           "description": "Built-in ID",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "SCALAR",
            |           "name": "Int",
            |           "description": "Built-in Int",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "SCALAR",
            |           "name": "JSON",
            |           "description": "A JSON scalar",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "INPUT_OBJECT",
            |           "name": "NadelBatchObjectIdentifiedBy",
            |           "description": "This is required by batch hydration to understand how to pull out objects from the batched result",
            |           "isOneOf": false,
            |           "fields": null,
            |           "inputFields": [
            |             {
            |               "name": "sourceId",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "resultId",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "INPUT_OBJECT",
            |           "name": "NadelHydrationArgument",
            |           "description": "This allows you to hydrate new values into fields",
            |           "isOneOf": false,
            |           "fields": null,
            |           "inputFields": [
            |             {
            |               "name": "name",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "value",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "JSON",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "INPUT_OBJECT",
            |           "name": "NadelHydrationCondition",
            |           "description": "Specify a condition for the hydration to activate",
            |           "isOneOf": false,
            |           "fields": null,
            |           "inputFields": [
            |             {
            |               "name": "result",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "INPUT_OBJECT",
            |                   "name": "NadelHydrationResultCondition",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "INPUT_OBJECT",
            |           "name": "NadelHydrationResultCondition",
            |           "description": "Specify a condition for the hydration to activate based on the result",
            |           "isOneOf": false,
            |           "fields": null,
            |           "inputFields": [
            |             {
            |               "name": "sourceField",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "predicate",
            |               "description": null,
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "INPUT_OBJECT",
            |                   "name": "NadelHydrationResultFieldPredicate",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "INPUT_OBJECT",
            |           "name": "NadelHydrationResultFieldPredicate",
            |           "description": null,
            |           "isOneOf": true,
            |           "fields": null,
            |           "inputFields": [
            |             {
            |               "name": "startsWith",
            |               "description": null,
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "equals",
            |               "description": null,
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "JSON",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "matches",
            |               "description": null,
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "Query",
            |           "description": null,
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "echo",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "SCALAR",
            |           "name": "String",
            |           "description": "Built-in String",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "__Directive",
            |           "description": null,
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "name",
            |               "description": "The __Directive type represents a Directive that a server supports.",
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "description",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "isRepeatable",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Boolean",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "locations",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "ENUM",
            |                       "name": "__DirectiveLocation",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "args",
            |               "description": null,
            |               "args": [
            |                 {
            |                   "name": "includeDeprecated",
            |                   "description": null,
            |                   "type": {
            |                     "kind": "SCALAR",
            |                     "name": "Boolean",
            |                     "ofType": null
            |                   },
            |                   "defaultValue": "false",
            |                   "isDeprecated": false,
            |                   "deprecationReason": null
            |                 }
            |               ],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "OBJECT",
            |                       "name": "__InputValue",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "ENUM",
            |           "name": "__DirectiveLocation",
            |           "description": "An enum describing valid locations where a directive can be placed",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": [
            |             {
            |               "name": "QUERY",
            |               "description": "Indicates the directive is valid on queries.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "MUTATION",
            |               "description": "Indicates the directive is valid on mutations.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "SUBSCRIPTION",
            |               "description": "Indicates the directive is valid on subscriptions.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "FIELD",
            |               "description": "Indicates the directive is valid on fields.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "FRAGMENT_DEFINITION",
            |               "description": "Indicates the directive is valid on fragment definitions.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "FRAGMENT_SPREAD",
            |               "description": "Indicates the directive is valid on fragment spreads.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "INLINE_FRAGMENT",
            |               "description": "Indicates the directive is valid on inline fragments.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "VARIABLE_DEFINITION",
            |               "description": "Indicates the directive is valid on variable definitions.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "SCHEMA",
            |               "description": "Indicates the directive is valid on a schema SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "SCALAR",
            |               "description": "Indicates the directive is valid on a scalar SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "OBJECT",
            |               "description": "Indicates the directive is valid on an object SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "FIELD_DEFINITION",
            |               "description": "Indicates the directive is valid on a field SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "ARGUMENT_DEFINITION",
            |               "description": "Indicates the directive is valid on a field argument SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "INTERFACE",
            |               "description": "Indicates the directive is valid on an interface SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "UNION",
            |               "description": "Indicates the directive is valid on an union SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "ENUM",
            |               "description": "Indicates the directive is valid on an enum SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "ENUM_VALUE",
            |               "description": "Indicates the directive is valid on an enum value SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "INPUT_OBJECT",
            |               "description": "Indicates the directive is valid on an input object SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "INPUT_FIELD_DEFINITION",
            |               "description": "Indicates the directive is valid on an input object field SDL definition.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "__EnumValue",
            |           "description": null,
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "name",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "description",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "isDeprecated",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Boolean",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "deprecationReason",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "__Field",
            |           "description": null,
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "name",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "description",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "args",
            |               "description": null,
            |               "args": [
            |                 {
            |                   "name": "includeDeprecated",
            |                   "description": null,
            |                   "type": {
            |                     "kind": "SCALAR",
            |                     "name": "Boolean",
            |                     "ofType": null
            |                   },
            |                   "defaultValue": "false",
            |                   "isDeprecated": false,
            |                   "deprecationReason": null
            |                 }
            |               ],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "OBJECT",
            |                       "name": "__InputValue",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "type",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "OBJECT",
            |                   "name": "__Type",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "isDeprecated",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Boolean",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "deprecationReason",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "__InputValue",
            |           "description": null,
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "name",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "description",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "type",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "OBJECT",
            |                   "name": "__Type",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "defaultValue",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "isDeprecated",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "Boolean",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "deprecationReason",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "__Schema",
            |           "description": "A GraphQL Introspection defines the capabilities of a GraphQL server. It exposes all available types and directives on the server, the entry points for query, mutation, and subscription operations.",
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "description",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "types",
            |               "description": "A list of all types supported by this server.",
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "OBJECT",
            |                       "name": "__Type",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "queryType",
            |               "description": "The type that query operations will be rooted at.",
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "OBJECT",
            |                   "name": "__Type",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "mutationType",
            |               "description": "If this server supports mutation, the type that mutation operations will be rooted at.",
            |               "args": [],
            |               "type": {
            |                 "kind": "OBJECT",
            |                 "name": "__Type",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "directives",
            |               "description": "A list of all directives supported by this server.",
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "OBJECT",
            |                       "name": "__Directive",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "subscriptionType",
            |               "description": "If this server support subscription, the type that subscription operations will be rooted at.",
            |               "args": [],
            |               "type": {
            |                 "kind": "OBJECT",
            |                 "name": "__Type",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "OBJECT",
            |           "name": "__Type",
            |           "description": null,
            |           "isOneOf": null,
            |           "fields": [
            |             {
            |               "name": "kind",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "ENUM",
            |                   "name": "__TypeKind",
            |                   "ofType": null
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "name",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "description",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "fields",
            |               "description": null,
            |               "args": [
            |                 {
            |                   "name": "includeDeprecated",
            |                   "description": null,
            |                   "type": {
            |                     "kind": "SCALAR",
            |                     "name": "Boolean",
            |                     "ofType": null
            |                   },
            |                   "defaultValue": "false",
            |                   "isDeprecated": false,
            |                   "deprecationReason": null
            |                 }
            |               ],
            |               "type": {
            |                 "kind": "LIST",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "NON_NULL",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "OBJECT",
            |                     "name": "__Field",
            |                     "ofType": null
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "interfaces",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "LIST",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "NON_NULL",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "OBJECT",
            |                     "name": "__Type",
            |                     "ofType": null
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "possibleTypes",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "LIST",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "NON_NULL",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "OBJECT",
            |                     "name": "__Type",
            |                     "ofType": null
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "enumValues",
            |               "description": null,
            |               "args": [
            |                 {
            |                   "name": "includeDeprecated",
            |                   "description": null,
            |                   "type": {
            |                     "kind": "SCALAR",
            |                     "name": "Boolean",
            |                     "ofType": null
            |                   },
            |                   "defaultValue": "false",
            |                   "isDeprecated": false,
            |                   "deprecationReason": null
            |                 }
            |               ],
            |               "type": {
            |                 "kind": "LIST",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "NON_NULL",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "OBJECT",
            |                     "name": "__EnumValue",
            |                     "ofType": null
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "inputFields",
            |               "description": null,
            |               "args": [
            |                 {
            |                   "name": "includeDeprecated",
            |                   "description": null,
            |                   "type": {
            |                     "kind": "SCALAR",
            |                     "name": "Boolean",
            |                     "ofType": null
            |                   },
            |                   "defaultValue": "false",
            |                   "isDeprecated": false,
            |                   "deprecationReason": null
            |                 }
            |               ],
            |               "type": {
            |                 "kind": "LIST",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "NON_NULL",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "OBJECT",
            |                     "name": "__InputValue",
            |                     "ofType": null
            |                   }
            |                 }
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "ofType",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "OBJECT",
            |                 "name": "__Type",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "isOneOf",
            |               "description": "This field is considered experimental because it has not yet been ratified in the graphql specification",
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "Boolean",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "specifiedByURL",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "specifiedByUrl",
            |               "description": null,
            |               "args": [],
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "isDeprecated": true,
            |               "deprecationReason": "This legacy name has been replaced by `specifiedByURL`"
            |             }
            |           ],
            |           "inputFields": null,
            |           "interfaces": [],
            |           "enumValues": null,
            |           "possibleTypes": null
            |         },
            |         {
            |           "kind": "ENUM",
            |           "name": "__TypeKind",
            |           "description": "An enum describing what kind of type a given __Type is",
            |           "isOneOf": null,
            |           "fields": null,
            |           "inputFields": null,
            |           "interfaces": null,
            |           "enumValues": [
            |             {
            |               "name": "SCALAR",
            |               "description": "Indicates this type is a scalar. `specifiedByURL` is a valid field",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "OBJECT",
            |               "description": "Indicates this type is an object. `fields` and `interfaces` are valid fields.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "INTERFACE",
            |               "description": "Indicates this type is an interface. `fields` and `possibleTypes` are valid fields.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "UNION",
            |               "description": "Indicates this type is a union. `possibleTypes` is a valid field.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "ENUM",
            |               "description": "Indicates this type is an enum. `enumValues` is a valid field.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "INPUT_OBJECT",
            |               "description": "Indicates this type is an input object. `inputFields` is a valid field.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "LIST",
            |               "description": "Indicates this type is a list. `ofType` is a valid field.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "NON_NULL",
            |               "description": "Indicates this type is a non-null. `ofType` is a valid field.",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "possibleTypes": null
            |         }
            |       ],
            |       "directives": [
            |         {
            |           "name": "include",
            |           "description": "Directs the executor to include this field or fragment only when the `if` argument is true",
            |           "locations": [
            |             "FIELD",
            |             "FRAGMENT_SPREAD",
            |             "INLINE_FRAGMENT"
            |           ],
            |           "args": [
            |             {
            |               "name": "if",
            |               "description": "Included when true.",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Boolean",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "skip",
            |           "description": "Directs the executor to skip this field or fragment when the `if` argument is true.",
            |           "locations": [
            |             "FIELD",
            |             "FRAGMENT_SPREAD",
            |             "INLINE_FRAGMENT"
            |           ],
            |           "args": [
            |             {
            |               "name": "if",
            |               "description": "Skipped when true.",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Boolean",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "hydrated",
            |           "description": "This allows you to hydrate new values into fields",
            |           "locations": [
            |             "FIELD_DEFINITION"
            |           ],
            |           "args": [
            |             {
            |               "name": "service",
            |               "description": "The target service",
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "field",
            |               "description": "The target top level field",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "identifiedBy",
            |               "description": "How to identify matching results",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "\"id\"",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "inputIdentifiedBy",
            |               "description": "How to identify matching results",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "INPUT_OBJECT",
            |                       "name": "NadelBatchObjectIdentifiedBy",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "defaultValue": "[]",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "indexed",
            |               "description": "Are results indexed",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Boolean",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "false",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "batchSize",
            |               "description": "The batch size",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Int",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "200",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "timeout",
            |               "description": "The timeout to use when completing hydration",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Int",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "-1",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "arguments",
            |               "description": "The arguments to the hydrated field",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "INPUT_OBJECT",
            |                       "name": "NadelHydrationArgument",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "when",
            |               "description": "Specify a condition for the hydration to activate",
            |               "type": {
            |                 "kind": "INPUT_OBJECT",
            |                 "name": "NadelHydrationCondition",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": true
            |         },
            |         {
            |           "name": "virtualType",
            |           "description": null,
            |           "locations": [
            |             "OBJECT"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "defaultHydration",
            |           "description": "This allows you to hydrate new values into fields",
            |           "locations": [
            |             "OBJECT",
            |             "INTERFACE"
            |           ],
            |           "args": [
            |             {
            |               "name": "field",
            |               "description": "The backing level field for the data",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "idArgument",
            |               "description": "Name of the ID argument on the backing field",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "identifiedBy",
            |               "description": "How to identify matching results",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "\"id\"",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "batchSize",
            |               "description": "The batch size",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Int",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "200",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "timeout",
            |               "description": "The timeout to use when completing hydration",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "Int",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "-1",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "idHydrated",
            |           "description": "This allows you to hydrate new values into fields",
            |           "locations": [
            |             "FIELD_DEFINITION"
            |           ],
            |           "args": [
            |             {
            |               "name": "idField",
            |               "description": "The field that holds the ID value(s) to hydrate",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "identifiedBy",
            |               "description": "(Optional override) how to identify matching results",
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "defaultValue": "null",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "stubbed",
            |           "description": "Allows you to introduce stubbed fields or types.\n\nStubbed fields or fields that return stubbed types _always_ return null.\n\nStubbed fields are meant to allow frontend clients consume new schema elements earlier so that they can iterate faster.",
            |           "locations": [
            |             "OBJECT",
            |             "FIELD_DEFINITION",
            |             "UNION",
            |             "ENUM",
            |             "INPUT_OBJECT"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "renamed",
            |           "description": "This allows you to rename a type or field in the overall schema",
            |           "locations": [
            |             "SCALAR",
            |             "OBJECT",
            |             "FIELD_DEFINITION",
            |             "INTERFACE",
            |             "UNION",
            |             "ENUM",
            |             "INPUT_OBJECT"
            |           ],
            |           "args": [
            |             {
            |               "name": "from",
            |               "description": "The type to be renamed",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "hidden",
            |           "description": "Indicates that the field is not available for queries or introspection",
            |           "locations": [
            |             "FIELD_DEFINITION"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "hydrationRemainingArguments",
            |           "description": null,
            |           "locations": [
            |             "ARGUMENT_DEFINITION"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "defer",
            |           "description": null,
            |           "locations": [
            |             "FRAGMENT_SPREAD",
            |             "INLINE_FRAGMENT"
            |           ],
            |           "args": [
            |             {
            |               "name": "if",
            |               "description": null,
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "Boolean",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             },
            |             {
            |               "name": "label",
            |               "description": null,
            |               "type": {
            |                 "kind": "SCALAR",
            |                 "name": "String",
            |                 "ofType": null
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "namespaced",
            |           "description": "Indicates that the field is a namespaced field.",
            |           "locations": [
            |             "FIELD_DEFINITION"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "partition",
            |           "description": "This allows you to partition a field",
            |           "locations": [
            |             "FIELD_DEFINITION"
            |           ],
            |           "args": [
            |             {
            |               "name": "pathToPartitionArg",
            |               "description": "The path to the split point",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "LIST",
            |                   "name": null,
            |                   "ofType": {
            |                     "kind": "NON_NULL",
            |                     "name": null,
            |                     "ofType": {
            |                       "kind": "SCALAR",
            |                       "name": "String",
            |                       "ofType": null
            |                     }
            |                   }
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "deprecated",
            |           "description": "Marks the field, argument, input field or enum value as deprecated",
            |           "locations": [
            |             "FIELD_DEFINITION",
            |             "ARGUMENT_DEFINITION",
            |             "ENUM_VALUE",
            |             "INPUT_FIELD_DEFINITION"
            |           ],
            |           "args": [
            |             {
            |               "name": "reason",
            |               "description": "The reason for the deprecation",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": "\"No longer supported\"",
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "specifiedBy",
            |           "description": "Exposes a URL that specifies the behaviour of this scalar.",
            |           "locations": [
            |             "SCALAR"
            |           ],
            |           "args": [
            |             {
            |               "name": "url",
            |               "description": "The URL that specifies the behaviour of this scalar.",
            |               "type": {
            |                 "kind": "NON_NULL",
            |                 "name": null,
            |                 "ofType": {
            |                   "kind": "SCALAR",
            |                   "name": "String",
            |                   "ofType": null
            |                 }
            |               },
            |               "defaultValue": null,
            |               "isDeprecated": false,
            |               "deprecationReason": null
            |             }
            |           ],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "oneOf",
            |           "description": "Indicates an Input Object is a OneOf Input Object.",
            |           "locations": [
            |             "INPUT_OBJECT"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         },
            |         {
            |           "name": "experimental_disableErrorPropagation",
            |           "description": "This directive disables error propagation when a non nullable field returns null for the given operation.",
            |           "locations": [
            |             "QUERY",
            |             "MUTATION",
            |             "SUBSCRIPTION"
            |           ],
            |           "args": [],
            |           "isRepeatable": false
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
