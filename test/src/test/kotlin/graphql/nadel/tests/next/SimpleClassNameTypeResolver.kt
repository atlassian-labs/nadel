package graphql.nadel.tests.next

import graphql.TypeResolutionEnvironment
import graphql.schema.GraphQLObjectType
import graphql.schema.TypeResolver

object SimpleClassNameTypeResolver : TypeResolver {
    override fun getType(env: TypeResolutionEnvironment): GraphQLObjectType {
        return env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
    }
}
