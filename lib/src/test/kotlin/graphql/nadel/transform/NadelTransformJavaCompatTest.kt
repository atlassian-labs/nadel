package graphql.nadel.transform

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.test.NadelTransformJavaCompatAdapter
import graphql.nadel.test.NadelTransformJavaCompatAdapter.TransformFieldContext
import graphql.nadel.test.NadelTransformJavaCompatAdapter.TransformOperationContext
import graphql.nadel.test.mock
import graphql.nadel.test.spy
import graphql.normalized.ExecutableNormalizedField
import io.mockk.confirmVerified
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertTrue

class NadelTransformJavaCompatTest {
    @Test
    fun `delegates name to compat`() = runTest {
        // Given
        val compat = spy(object : NadelTransformJavaCompatAdapter {
            override val name: String
                get() = "Test"
        })

        val transformer = NadelTransformJavaCompat.create(compat)

        // When
        val name = transformer.name

        // Then
        assertTrue(name == "Test")
        verify(exactly = 1) { compat.name }

        confirmVerified(compat)
    }

    @Test
    fun `delegates getTransformOperationContext to compat`() = runTest {
        class Prop(
            parentContext: NadelOperationExecutionContext,
            val prop: String,
        ) : TransformOperationContext(parentContext)

        // Given
        val compat = spy(object : NadelTransformJavaCompatAdapter {
            override val name: String
                get() = "Test"

            override fun getTransformOperationContext(
                operationExecutionContext: NadelOperationExecutionContext,
            ): CompletableFuture<TransformOperationContext> {
                return CompletableFuture.completedFuture(Prop(operationExecutionContext, "something"))
            }
        })

        val transformer = NadelTransformJavaCompat.create(compat)

        val operationExecutionContext = mock<NadelOperationExecutionContext>()

        // When
        val transformServiceExecutionContext = transformer.getTransformOperationContext(
            operationExecutionContext,
        )

        // Then
        assertTrue(transformServiceExecutionContext is Prop)
        assertTrue(transformServiceExecutionContext.prop == "something")

        verify(exactly = 1) {
            compat.getTransformOperationContext(
                operationExecutionContext,
            )
        }

        confirmVerified(compat)
    }

    @Test
    fun `delegates getTransformFieldContext to compat`() = runTest {
        // Given
        data class MyTransformFieldContext(
            override val parentContext: TransformOperationContext,
            override val overallField: ExecutableNormalizedField,
            val data: Int,
        ) : TransformFieldContext(parentContext, overallField)

        val compat = spy(object : NadelTransformJavaCompatAdapter {
            override val name: String
                get() = "Test"

            override fun getTransformFieldContext(
                transformContext: TransformOperationContext,
                overallField: ExecutableNormalizedField,
            ): CompletableFuture<TransformFieldContext?> {
                return CompletableFuture.completedFuture(MyTransformFieldContext(transformContext, overallField, 123))
            }
        })

        val transformer = NadelTransformJavaCompat.create(compat)

        val transformOperationContext = mock<TransformOperationContext>()
        val overallField = mock<ExecutableNormalizedField>()

        // When
        val fieldContext = transformer.getTransformFieldContext(
            transformOperationContext,
            overallField,
        )

        // Then
        assertTrue(fieldContext is MyTransformFieldContext)
        assertTrue(fieldContext.data == 123)

        verify(exactly = 1) {
            compat.getTransformFieldContext(
                transformOperationContext,
                overallField,
            )
        }

        confirmVerified(compat)
    }

    @Test
    fun `delegates transformField to compat`() = runTest {
        // Given
        val expectedResult = mock<NadelTransformFieldResult>()
        val compat = spy(object : NadelTransformJavaCompatAdapter {
            override val name: String
                get() = "Test"

            override fun transformField(
                transformContext: TransformFieldContext,
                transformer: NadelQueryTransformerJavaCompat,
                field: ExecutableNormalizedField,
            ): CompletableFuture<NadelTransformFieldResult> {
                return CompletableFuture.completedFuture(expectedResult)
            }
        })

        val transformer = NadelTransformJavaCompat.create(compat)

        val queryTransformer = mock<NadelQueryTransformer>()
        val field = mock<ExecutableNormalizedField>()
        val transformFieldContext = mock<TransformFieldContext>()

        // When
        val transformField = transformer.transformField(
            transformFieldContext,
            queryTransformer,
            field,
        )

        // Then
        assertTrue(transformField === expectedResult)

        verify(exactly = 1) {
            compat.transformField(
                transformFieldContext,
                // This is another wrapper for Java compat
                match { it.queryTransformer == queryTransformer },
                field,
            )
        }

        confirmVerified(compat)
    }

    @Test
    fun `delegates transformResult to compat`() = runTest {
        // Given
        val expectedResult = listOf<NadelResultInstruction>(
            NadelResultInstruction.Set(
                subject = JsonNode(mutableMapOf<String, Any?>()),
                key = NadelResultKey("hello"),
                newValue = JsonNode(1),
            )
        )
        val compat = spy(object : NadelTransformJavaCompatAdapter {
            override val name: String
                get() = "Test"

            override fun transformResult(
                transformContext: TransformFieldContext,
                underlyingParentField: ExecutableNormalizedField?,
                resultNodes: JsonNodes,
            ): CompletableFuture<List<NadelResultInstruction>> {
                return CompletableFuture.completedFuture(expectedResult)
            }
        })

        val transformer = NadelTransformJavaCompat.create(compat)

        val underlyingParentField = mock<ExecutableNormalizedField>()
        val resultNodes = mock<JsonNodes>()
        val transformFieldContext = mock<TransformFieldContext>()

        // When
        val getResultInstructions = transformer.transformResult(
            transformContext = transformFieldContext,
            underlyingParentField = underlyingParentField,
            resultNodes = resultNodes,
        )

        // Then
        assertTrue(getResultInstructions === expectedResult)

        verify(exactly = 1) {
            compat.transformResult(
                transformContext = transformFieldContext,
                underlyingParentField = underlyingParentField,
                resultNodes = resultNodes,
            )
        }

        confirmVerified(compat)
    }

    @Test
    fun `delegates onComplete to compat`() = runTest {
        // Given
        val compat = spy(object : NadelTransformJavaCompatAdapter {
            override val name: String
                get() = "Test"

            override fun onComplete(
                transformContext: TransformOperationContext,
                resultNodes: JsonNodes,
            ): CompletableFuture<Void> {
                return CompletableFuture.completedFuture(null)
            }
        })

        val transformer = NadelTransformJavaCompat.create(compat)

        val resultNodes = mock<JsonNodes>()
        val transformOperationContext = mock<TransformOperationContext>()

        // When
        val onComplete = transformer.onComplete(
            transformOperationContext,
            resultNodes,
        )

        // Then
        assertTrue(onComplete === Unit)

        verify(exactly = 1) {
            compat.onComplete(
                transformOperationContext,
                resultNodes,
            )
        }

        confirmVerified(compat)
    }
}
