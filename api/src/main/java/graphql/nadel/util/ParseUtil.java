package graphql.nadel.util;

import graphql.PublicApi;
import graphql.nadel.schema.ServiceSchemaProblem;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;

import java.io.Reader;

import static java.lang.String.format;

@PublicApi
public class ParseUtil {

    /**
     * This can parse SDL text and produce a {@link graphql.schema.idl.TypeDefinitionRegistry} from that text
     *
     * @param serviceName the name of the service to do this on behalf of
     * @param sdl         the SDL text
     *
     * @return a TypeDefinitionRegistry
     *
     * @throws graphql.nadel.schema.ServiceSchemaProblem in the name of the specified service
     * @see graphql.parser.MultiSourceReader
     */
    public static TypeDefinitionRegistry parseServiceSDL(String serviceName, Reader sdl) throws ServiceSchemaProblem {
        try {
            return new SchemaParser().parse(sdl);
        } catch (SchemaProblem schemaProblem) {
            String message = format("There was a problem parsing the schema SDL for '%s' : %s",
                    serviceName, schemaProblem.getMessage());
            throw new ServiceSchemaProblem(message, serviceName, schemaProblem);
        }
    }

}
