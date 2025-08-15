package graphql.nadel.engine.transform

import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters

internal data class NadelTransformTimingSteps(
    val executionPlan: NadelInstrumentationTimingParameters.Step,
    val queryTransform: NadelInstrumentationTimingParameters.Step,
    val resultTransform: NadelInstrumentationTimingParameters.Step,
)
