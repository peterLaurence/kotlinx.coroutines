/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental

import kotlin.coroutines.experimental.*

@Suppress("FunctionName")
public fun SupervisorJob(parent: Job? = null) : Job = SupervisorJobImpl(parent)

public suspend fun <R> supervisorScope(block: suspend CoroutineScope.() -> R): R {
    // todo: optimize implementation to a single allocated object
    // todo: fix copy-and-paste with coroutineScope
    val owner = SupervisorCoroutine<R>(coroutineContext)
    owner.start(CoroutineStart.UNDISPATCHED, owner, block)
    owner.join()
    if (owner.isCancelled) {
        throw owner.getCancellationException().let { it.cause ?: it }
    }
    val state = owner.state
    if (state is CompletedExceptionally) {
        throw state.cause
    }
    @Suppress("UNCHECKED_CAST")
    return state as R

}

private class SupervisorJobImpl(parent: Job?) : JobSupport(true) {
    init { initParentJobInternal(parent) }
    override val onFailComplete get() = true
    override val handlesException: Boolean get() = false
    override fun childFailed(cause: Throwable): Boolean = false
}

private class SupervisorCoroutine<R>(
    parentContext: CoroutineContext
) : AbstractCoroutine<R>(parentContext, true) {
    override fun childFailed(cause: Throwable): Boolean = false
}
