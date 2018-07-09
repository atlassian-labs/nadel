grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL: serviceDefinition+;

serviceDefinition:
'service' name '{' typeSystemDefinition* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? fieldTransformation? directives? fieldsDefinition?;

fieldDefinition : description? name argumentsDefinition? ':' type fieldTransformation? directives?;

// fixme: this allows for an empty arrow -- first shot at fixing ( target remote? | remote ) failed
fieldTransformation : '<=' targetFieldDefinition? remoteCallDefinition? inputMappingDefinition? innerMappingDefinition? ;

inputMappingDefinition : '$input.' name ;

innerMappingDefinition: '$inner.' name;

targetFieldDefinition : name ':' type;

remoteCallDefinition : '{' remoteQuery '(' remoteArgument remoteInput? ')' '}' ;

remoteQuery : name ;

remoteArgument : name ;

remoteInput: ':' name ;


