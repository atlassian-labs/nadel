grammar StitchingDSL;
import GraphqlSDL;

@header {
    package graphql.nadel.parser.antlr;
}

stitchingDSL: serviceDefinition+;

serviceDefinition:
'service' name '{' typeSystemDefinition* '}' ;

objectTypeDefinition : description? TYPE name implementsInterfaces? typeTransformation? directives? fieldsDefinition? ;

fieldDefinition : description? name argumentsDefinition? ':' type fieldTransformation? directives?;

// fixme: this allows for an empty arrow -- first shot at fixing ( target remote? | remote ) failed
fieldTransformation : '<=' remoteCallDefinition? inputMappingDefinition? innerServiceTransformation? ;

typeTransformation : '<=' innerTypeTransformation ;

inputMappingDefinition : '$input.' name ;

innerTypeTransformation: '$inner.' name;

innerServiceTransformation: '$inner.' name;

targetFieldDefinition : name ':' type;

remoteCallDefinition : '{' remoteQuery '(' remoteArgument remoteInput? ')' '}' ;

remoteQuery : name ;

remoteArgument : name ;

remoteInput: ':' name ;


