grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL: definition+;

definition:
serviceDefinition
;

serviceDefinition:
'service' name '{' serviceUrl typeSystemDefinition* '}' ;
serviceUrl: 'url' ':' stringValue;


fieldDefinition : description? name argumentsDefinition? ':' type fieldTransformation? directives?;

// fixme: this allows for an empty arrow -- first shot at fixing ( target remote? | remote ) failed
fieldTransformation : '<=' targetFieldDefinition? remoteCallDefinition?;

targetFieldDefinition : name ':' type;

remoteCallDefinition : '{' remoteQuery '(' remoteArgument remoteInput? ')' '}' ;

remoteQuery : name ;

remoteArgument : name ;

remoteInput: ':' name ;



