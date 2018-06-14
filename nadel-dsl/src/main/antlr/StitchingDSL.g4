grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL: definition+;

definition:
serviceDefinition|extendServiceDefinition
;

serviceDefinition:
'service' name '{' serviceUrl typeSystemDefinition* '}' ;

extendServiceDefinition:
'extend' 'service' name '{' serviceUrl objectTypeExtensionDefinition* '}' ;

serviceUrl: 'serviceUrl' ':' stringValue;

fieldsDefinition : '{' (fieldDefinition|linkedField)+ '}';

linkedField: '_=>' linkDefinition;

linkDefinition: 'from' topLevelField '(' argumentName ')' 'with input' variableName 'as' fieldName added?;

added: 'added';
topLevelField: name;
argumentName: name;
variableName: name;
fieldName: name;

fieldDefinition : description? name argumentsDefinition? ':' type fieldTransformation? directives?;

// fixme: this allows for an empty arrow -- first shot at fixing ( target remote? | remote ) failed
fieldTransformation : '=>' linkDefinition;




