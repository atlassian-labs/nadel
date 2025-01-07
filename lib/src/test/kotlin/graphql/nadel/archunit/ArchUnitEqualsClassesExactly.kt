package graphql.nadel.archunit

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction
import kotlin.reflect.KClass

fun <T : Any> GivenClassesConjunction.equalsExactly(
    vararg requiredClasses: KClass<out T>,
): ClassesShouldConjunction {
    return should(ArchUnitEqualsClassesExactly(requiredClasses.map { it.java }))
}

class ArchUnitEqualsClassesExactly<T : Any>(
    private val requiredClasses: List<Class<out T>>,
) : ArchCondition<JavaClass>("equal exactly the given classes") {
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
