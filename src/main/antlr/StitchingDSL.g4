grammar StitchingDSL;
import Graphql;

@header {
    package graphql.nadel.parser.antlr;
}
// Document

// todo: move some of that within service definitions
stitchingDSL: definition+;

definition:
operationDefinition |
fragmentDefinition |
typeSystemDefinition |
serviceDefinition
;

serviceDefinition:
'service' name '{' serviceUrl typeSystemDefinition* '}' ;
serviceUrl: 'url' ':' stringValue;

operationDefinition:
selectionSet |
operationType  name? variableDefinitions? directives? selectionSet;


