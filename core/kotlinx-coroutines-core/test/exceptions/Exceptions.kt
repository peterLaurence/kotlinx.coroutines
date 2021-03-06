/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental.exceptions

import kotlinx.coroutines.experimental.*
import java.io.*
import java.util.*
import kotlin.coroutines.experimental.*
import kotlin.test.*

/**
 * Proxy for [Throwable.getSuppressed] for tests, which are compiled for both JDK 1.6 and JDK 1.8,
 * but run only under JDK 1.8
 */
fun Throwable.suppressed(): Array<Throwable> {
    val method = this::class.java.getMethod("getSuppressed") ?: error("This test can only be run using JDK 1.7")
    @Suppress("UNCHECKED_CAST")
    return method.invoke(this) as Array<Throwable>
}

internal inline fun <reified T : Throwable> checkException(exception: Throwable): Boolean {
    assertTrue(exception is T)
    assertTrue(exception.suppressed().isEmpty())
    assertNull(exception.cause)
    return true
}

internal fun checkCycles(t: Throwable) {
    val sw = StringWriter()
    t.printStackTrace(PrintWriter(sw))
    assertFalse(sw.toString().contains("CIRCULAR REFERENCE"))
}

class CapturingHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
    CoroutineExceptionHandler {
    val unhandled: MutableList<Throwable> = Collections.synchronizedList(ArrayList<Throwable>())!!

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        unhandled.add(exception)
    }

    fun getException(): Throwable {
        val size = unhandled.size
        assert(size == 1) { "Expected one unhandled exception, but have $size: $unhandled" }
        return unhandled[0]
    }
}

internal fun runBlock(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Throwable {
    val handler = CapturingHandler()
    runBlocking(context + handler, block = block)
    return handler.getException()
}

internal fun runBlockForMultipleExceptions(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): List<Throwable> {
    val handler = CapturingHandler()
    runBlocking(context + handler, block = block)
    return handler.unhandled
}
