package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.execution.ExecutionId
import graphql.execution.nextgen.ExecutionHelper
import graphql.language.AstPrinter
import graphql.nadel.DefinitionRegistry
import graphql.nadel.FieldInfo
import graphql.nadel.FieldInfos
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.TestUtil
import graphql.nadel.dsl.ServiceDefinition
import graphql.schema.GraphQLFieldDefinition
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class NadelExecutionStrategyTest extends Specification {


    ExecutionHelper executionHelper = new ExecutionHelper()

    def "one call to one service"() {
        given:
        def serviceSchema1 = TestUtil.schema("""
        type Query {
            foo: String  
        }
        """)

        def overallSchema = TestUtil.schema("""
        type Query {
            foo: String
        }
        """)
        def serviceExecution = Mock(ServiceExecution)
        def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        def definitionRegistry = Mock(DefinitionRegistry)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service1", serviceSchema1, serviceExecution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema)


        def query = "{foo}"
        def document = TestUtil.parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null);
        executionData.executionContext

        def expectedQuery = "query {foo}"

        when:
        nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * serviceExecution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery
        }) >> CompletableFuture.completedFuture(Mock(ServiceExecutionResult))
    }

    FieldInfos topLevelFieldInfo(GraphQLFieldDefinition fieldDefinition, Service service) {
        FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, fieldDefinition)
        return new FieldInfos([(fieldDefinition): fieldInfo])
    }

}
