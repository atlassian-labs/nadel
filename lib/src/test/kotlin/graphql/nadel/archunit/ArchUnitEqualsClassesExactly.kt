package graphql.nadel.archunit

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import kotlin.reflect.KClass

class ArchUnitEqualsClassesExactly<T : Any>(
    private val requiredClasses: List<Class<out T>>,
) : ArchCondition<JavaClass>("equal exactly the given classes") {
    constructor(vararg requiredClasses: Class<out T>) : this(requiredClasses.toList())

    constructor(vararg requiredClasses: KClass<out T>) : this(requiredClasses.map { it.java })

    private val actualClasses: MutableList<JavaClass> = mutableListOf()

    override fun check(item: JavaClass, events: ConditionEvents) {
        actualClasses.add(item)
    }

    override fun finish(events: ConditionEvents) {
        val requiredClassNames = requiredClasses.mapTo(LinkedHashSet()) { it.name }
        val actualClassNames = actualClasses.mapTo(LinkedHashSet()) { it.fullName }

        val isValid = requiredClassNames == actualClassNames
        if (isValid) {
            events.add(
                SimpleConditionEvent(
                    requiredClassNames,
                    true,
                    "Classes match $requiredClassNames",
                ),
            )
        } else {
            val extraClassNames = actualClassNames - requiredClassNames
            val missingClassNames = requiredClassNames - actualClassNames

            if (extraClassNames.isNotEmpty()) {
                events.add(
                    SimpleConditionEvent(
                        requiredClassNames,
                        false,
                        "Found extra classes $extraClassNames",
                    ),
                )
            }
            if (missingClassNames.isNotEmpty()) {
                events.add(
                    SimpleConditionEvent(
                        requiredClassNames,
                        false,
                        "Missing classes $missingClassNames"
                    ),
                )
            }
        }
    }
}
