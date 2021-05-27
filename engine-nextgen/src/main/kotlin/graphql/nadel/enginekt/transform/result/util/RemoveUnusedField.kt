package graphql.nadel.enginekt.transform.result.util

import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.JsonMap
import graphql.normalized.NormalizedField

object RemoveUnusedField {
    private const val typeNameFieldName = "__typename"

    fun getInstructions(
        node: JsonNode,
        originalSelection: NormalizedField,
        pathToField: List<String>,
    ): List<NadelResultInstruction> {
        @Suppress("UNCHECKED_CAST")
        val map = node.value as JsonMap? ?: return emptyList()

        val pathToUnused = getUnusedPath(map, originalSelection, pathToField, pathToFieldIndex = 0)
            ?: return emptyList()

        return JsonNodeExtractor.getNodesAt(node, pathToUnused)
            .map {
                NadelResultInstruction.Remove(it.path)
            }
    }

    private fun getUnusedPath(
        parentMap: JsonMap,
        parentField: NormalizedField,
        pathToField: List<String>,
        pathToFieldIndex: Int = 0,
    ): List<String>? {
        val typeName = parentMap[typeNameFieldName] ?: return emptyList()
        val fieldName = pathToField[pathToFieldIndex]

        val existingField = parentField.children.find {
            it.objectTypeNames.contains(typeName) && it.fieldName == fieldName
        }

        return when {
            // Field is unused, return path to it
            existingField == null -> pathToField.subList(0, pathToFieldIndex + 1) // +1 for end inclusive
            // No more fields to check
            pathToFieldIndex == pathToField.lastIndex -> null
            // Check whether next field is used or not
            else -> {
                @Suppress("UNCHECKED_CAST")
                val mapAtField = parentMap[fieldName] as JsonMap? ?: return null
                getUnusedPath(
                    parentMap = mapAtField,
                    parentField = existingField,
                    pathToField = pathToField,
                    pathToFieldIndex = pathToFieldIndex + 1,
                )
            }
        }
    }
}
