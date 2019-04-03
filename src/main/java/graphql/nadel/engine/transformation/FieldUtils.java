package graphql.nadel.engine.transformation;

import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.List;
import java.util.Map;

public final class FieldUtils {

    public static String resultKeyForField(Field field) {
        return field.getAlias() != null ? field.getAlias() : field.getName();
    }

    public static boolean hasFieldSubSelectionAtIndex(String fieldName, Field parentField, int desiredIndex) {
        SelectionSet selectionSet = parentField.getSelectionSet();
        if (selectionSet != null) {
            List<Selection> selections = selectionSet.getSelections();
            for (int i = 0; i < selections.size(); i++) {
                Selection selection = selections.get(i);
                if (selection instanceof Field) {
                    Field subSelectionField = (Field) selection;
                    if (fieldName.equals(subSelectionField.getName())) {
                        return i == desiredIndex;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasUnAliasedFieldSubSelection(String fieldName, MergedField parentField, Map<String, FragmentDefinition> fragmentsByName) {
        for (Field field : parentField.getFields()) {
            boolean hasSubSelection = hasUnAliasedFieldSubSelection(fieldName, field, fragmentsByName);
            if (hasSubSelection) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasUnAliasedFieldSubSelection(String fieldName, Field parentField, Map<String, FragmentDefinition> fragmentsByName) {
        SelectionSet selectionSet = parentField.getSelectionSet();
        return selectionSetHasFieldAnywhere(fieldName, null, selectionSet, fragmentsByName, 0, 1);
    }

    private static boolean selectionSetHasFieldAnywhere(String fieldName, String alias, SelectionSet selectionSet, Map<String, FragmentDefinition> fragmentsByName, int depth, int maxDepth) {
        if (depth > maxDepth) {
            return false;
        }
        if (selectionSet != null) {
            List<Selection> selections = selectionSet.getSelections();
            for (Selection selection : selections) {
                if (selection instanceof Field) {
                    Field subSelectionField = (Field) selection;
                    if (fieldName.equals(subSelectionField.getName())) {
                        if (subSelectionField.getAlias() == null) {
                            return alias == null;
                        } else {
                            return subSelectionField.getAlias().equals(alias);
                        }
                    }
                } else if (selection instanceof InlineFragment) {
                    InlineFragment inlineFragment = (InlineFragment) selection;
                    return selectionSetHasFieldAnywhere(fieldName, alias, inlineFragment.getSelectionSet(), fragmentsByName, ++depth, maxDepth);
                } else if (selection instanceof FragmentSpread) {
                    FragmentSpread fragmentSpread = (FragmentSpread) selection;
                    FragmentDefinition fragmentDefinition = fragmentsByName.get(fragmentSpread.getName());
                    if (fragmentDefinition != null) {
                        return selectionSetHasFieldAnywhere(fieldName, alias, fragmentDefinition.getSelectionSet(), fragmentsByName, ++depth, maxDepth);
                    }
                }
            }
            return false;
        }
        return false;
    }

}
