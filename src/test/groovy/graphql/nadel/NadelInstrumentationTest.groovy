package graphql.nadel

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.instrumentation.ChainedNadelInstrumentation
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters
import graphql.nadel.testutils.TestUtil
import graphql.validation.ValidationError
import spock.lang.Specification

import static graphql.execution.instrumentation.SimpleInstrumentationContext.noOp
import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelInstrumentationTest extends Specification {

    def simpleNDSL = """
         service MyService {
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
            type Mutation{
                hello: String  
            } 
         }
        """

    def simpleUnderlyingSchema = typeDefinitions("""
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
            type Mutation{
                hello: String  
            } 
        """)

    def delegatedExecution = Mock(ServiceExecution)
    def serviceFactory = TestUtil.serviceFactory(delegatedExecution, simpleUnderlyingSchema)

    class TestState implements InstrumentationState {
    }

    def "instrumentation is called"() {

        given:
        def query = """
        query OpName { hello {name} hello {id} }
        """

        def variables = ["var1": "val1"]

        def instrumentationCalled = false
        def instrumentationParseCalled = false
        def instrumentationValidateCalled = false
        def instrumentationExecuteCalled = false
        NadelInstrumentation instrumentation = new NadelInstrumentation() {
            @Override
            InstrumentationState createState(NadelInstrumentationCreateStateParameters parameters) {
                return new TestState()
            }

            @Override
            InstrumentationContext<ExecutionResult> beginQueryExecution(NadelInstrumentationQueryExecutionParameters parameters) {
                instrumentationCalled = true
                parameters.instrumentationState instanceof TestState
                parameters.query == query
                parameters.variables == variables
                parameters.operation == "OpName"
                noOp()
            }

            @Override
            InstrumentationContext<Document> beginParse(NadelInstrumentationQueryExecutionParameters parameters) {
                instrumentationParseCalled = true
                noOp()
            }

            @Override
            InstrumentationContext<List<ValidationError>> beginValidation(NadelNadelInstrumentationQueryValidationParameters parameters) {
                instrumentationValidateCalled = true
                noOp()
            }

            @Override
            InstrumentationContext<ExecutionResult> beginExecute(NadelInstrumentationExecuteOperationParameters parameters) {
                instrumentationExecuteCalled = true
                noOp()
            }
        }

        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .instrumentation(instrumentation)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .variables(variables)
                .context("contextObj")
                .operationName("OpName")
                .build()
        when:
        nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            def data = [hello: [id: "3", name: "earth"]]
            completedFuture(new ServiceExecutionResult(data))
        }
        instrumentationCalled
        instrumentationParseCalled
        instrumentationValidateCalled
        instrumentationExecuteCalled
    }

    def "chained instrumentation works as expected"() {

        given:
        def query = """
        query OpName { hello {name} hello {id} }
        """

        def variables = ["var1": "val1"]

        def instrumentationCalled = 0
        def instrumentationParseCalled = 0
        def instrumentationValidateCalled = 0
        def instrumentationExecuteCalled = 0
        NadelInstrumentation instrumentation = new NadelInstrumentation() {
            @Override
            InstrumentationState createState(NadelInstrumentationCreateStateParameters parameters) {
                return new TestState()
            }

            @Override
            InstrumentationContext<ExecutionResult> beginQueryExecution(NadelInstrumentationQueryExecutionParameters parameters) {
                instrumentationCalled++
                parameters.instrumentationState instanceof TestState
                parameters.query == query
                parameters.variables == variables
                parameters.operation == "OpName"
                noOp()
            }

            @Override
            InstrumentationContext<Document> beginParse(NadelInstrumentationQueryExecutionParameters parameters) {
                instrumentationParseCalled++
                noOp()
            }

            @Override
            InstrumentationContext<List<ValidationError>> beginValidation(NadelNadelInstrumentationQueryValidationParameters parameters) {
                instrumentationValidateCalled++
                noOp()
            }

            @Override
            InstrumentationContext<ExecutionResult> beginExecute(NadelInstrumentationExecuteOperationParameters parameters) {
                instrumentationExecuteCalled++
                noOp()
            }
        }

        ChainedNadelInstrumentation chainedInstrumentation = new ChainedNadelInstrumentation(
                [instrumentation, instrumentation]
        )



        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .instrumentation(chainedInstrumentation)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .variables(variables)
                .context("contextObj")
                .operationName("OpName")
                .build()
        when:
        nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            def data = [hello: [id: "3", name: "earth"]]
            completedFuture(new ServiceExecutionResult(data))
        }
        instrumentationCalled == 2
        instrumentationParseCalled == 2
        instrumentationValidateCalled == 2
        instrumentationExecuteCalled == 2
    }
}
