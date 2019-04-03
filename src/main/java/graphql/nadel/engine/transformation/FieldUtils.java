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

    /**
     * This returns the aliased result name if a field is alised other its the field name
     *
     * @param field the field in play
     *
     * @return the result name
     */
    public static String resultKeyForField(Field field) {
        return field.getAlias() != null ? field.getAlias() : field.getName();
    }

    /**
     * Returns true if the parent field has a direct field sub selection (field only) at the specified index
     *
     * @param subFieldName the name of the direct sub field to check for
     * @param parentField  the parent field
     * @param desiredIndex the index you want the result to be at
     *
     * @return true if its at that index as a direct sub field
     */
    public static boolean hasFieldSubSelectionAtIndex(String subFieldName, Field parentField, int desiredIndex) {
        SelectionSet selectionSet = parentField.getSelectionSet();
        if (selectionSet != null) {
            List<Selection> selections = selectionSet.getSelections();
            for (int i = 0; i < selections.size(); i++) {
                Selection selection = selections.get(i);
                if (selection instanceof Field) {
                    Field subSelectionField = (Field) selection;
                    if (subFieldName.equals(subSelectionField.getName())) {
                        return i == desiredIndex;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the parent field has the specified sub field name on it (without an alias) in the direct sub selection (eg one level down only)
     *
     * @param subFieldName    the sub field name to find
     * @param parentField     the parent field to check
     * @param fragmentsByName the map of fragments so they can be resolved
     *
     * @return true if the parent field has the named direct sub field selection without aliasing
     */
    public static boolean hasUnAliasedFieldSubSelection(String subFieldName, MergedField parentField, Map<String, FragmentDefinition> fragmentsByName) {
        for (Field field : parentField.getFields()) {
            boolean hasSubSelection = hasUnAliasedFieldSubSelection(subFieldName, field, fragmentsByName);
            if (hasSubSelection) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the parent field has the specified sub field name on it (without an alias) in the direct sub selection (eg one level down only)
     *
     * @param subFieldName    the sub field name to find
     * @param parentField     the parent field to check
     * @param fragmentsByName the map of fragments so they can be resolved
     *
     * @return true if the parent field has the named direct sub field selection without aliasing
     */
    public static boolean hasUnAliasedFieldSubSelection(String subFieldName, Field parentField, Map<String, FragmentDefinition> fragmentsByName) {
        SelectionSet selectionSet = parentField.getSelectionSet();
        return selectionSetHasFieldAnywhere(subFieldName, null, selectionSet, fragmentsByName, 0, 1);
    }

    private static boolean selectionSetHasFieldAnywhere(String subFieldName, String alias, SelectionSet selectionSet, Map<String, FragmentDefinition> fragmentsByName, int depth, int maxDepth) {
        if (depth > maxDepth) {
            return false;
        }
        if (selectionSet != null) {
            List<Selection> selections = selectionSet.getSelections();
            for (Selection selection : selections) {
                if (selection instanceof Field) {
                    Field subSelectionField = (Field) selection;
                    if (subFieldName.equals(subSelectionField.getName())) {
                        if (subSelectionField.getAlias() == null) {
                            return alias == null;
                        } else {
                            return subSelectionField.getAlias().equals(alias);
                        }
                    }
                } else if (selection instanceof InlineFragment) {
                    InlineFragment inlineFragment = (InlineFragment) selection;
                    boolean found = selectionSetHasFieldAnywhere(subFieldName, alias, inlineFragment.getSelectionSet(), fragmentsByName, depth+1, maxDepth);
                    if (found) {
                        return true;
                    }
                } else if (selection instanceof FragmentSpread) {
                    FragmentSpread fragmentSpread = (FragmentSpread) selection;
                    FragmentDefinition fragmentDefinition = fragmentsByName.get(fragmentSpread.getName());
                    if (fragmentDefinition != null) {
                        boolean found = selectionSetHasFieldAnywhere(subFieldName, alias, fragmentDefinition.getSelectionSet(), fragmentsByName, depth+1, maxDepth);
                        if (found) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

}
