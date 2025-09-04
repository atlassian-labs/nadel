package graphql.nadel.archunit

import com.tngtech.archunit.core.domain.JavaClass

inline fun <reified T : Any> JavaClass.isAssignableTo(): Boolean {
    return isAssignableTo(T::class.java)
}
