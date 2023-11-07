/**
 * This file contains random code to make Mockk slightly more sane.
 */
package graphql.nadel.test

import io.mockk.Call
import io.mockk.MockK
import io.mockk.MockKDsl
import kotlin.reflect.KClass

inline fun <reified T : Any> mock(
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    relaxUnitFun: Boolean = false,
    block: (T) -> Unit = {},
): T = MockK.useImpl {
    MockKDsl.internalMockk(
        name,
        relaxed,
        moreInterfaces,
        relaxUnitFun = relaxUnitFun,
        block = block
    )
}

/**
 * Builds a new spy for specified class. Initializes object via default constructor.
 *
 * A spy is a special kind of mockk that enables a mix of mocked behaviour and real behaviour.
 * A part of the behaviour may be mocked, but any non-mocked behaviour will call the original method.
 *
 * @param name spyk name
 * @param moreInterfaces additional interfaces for this spyk to implement
 * @param recordPrivateCalls allows this spyk to record any private calls, enabling a verification
 * @param block block to execute after spyk is created with spyk as a receiver
 *
 */
inline fun <reified T : Any> spy(
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    recordPrivateCalls: Boolean = false,
    block: (T) -> Unit = {},
): T = MockK.useImpl {
    MockKDsl.internalSpyk(
        name,
        moreInterfaces,
        recordPrivateCalls = recordPrivateCalls,
        block = block
    )
}

/**
 * Builds a new spy for specified class, copying fields from [objToCopy].
 *
 * A spy is a special kind of mockk that enables a mix of mocked behaviour and real behaviour.
 * A part of the behaviour may be mocked, but any non-mocked behaviour will call the original method.
 *
 */
inline fun <reified T : Any> spy(
    objToCopy: T,
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    recordPrivateCalls: Boolean = false,
    block: (T) -> Unit = {},
): T = MockK.useImpl {
    MockKDsl.internalSpyk(
        objToCopy,
        name,
        moreInterfaces,
        recordPrivateCalls = recordPrivateCalls,
        block = block
    )
}

inline fun <reified T> Call.firstArg() = invocation.args[0] as T
inline fun <reified T> Call.secondArg() = invocation.args[1] as T
inline fun <reified T> Call.thirdArg() = invocation.args[2] as T
inline fun <reified T> Call.lastArg() = invocation.args.last() as T
inline fun <reified T> Call.arg(n: Int) = invocation.args[n] as T
