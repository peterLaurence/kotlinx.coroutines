/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NAMED_ARGUMENTS_NOT_ALLOWED") // KT-21913

package kotlinx.coroutines.experimental

import kotlin.test.*

class SupervisorTest : TestBase() {
    @Test
    fun testSupervisorJob() = runTest(
        unhandled = listOf(
            { it -> it is TestException2 },
            { it -> it is TestException1 }
        )
    ) {
        expect(1)
        val supervisor = SupervisorJob()
        val job1 = launch(supervisor + CoroutineName("job1")) {
            expect(2)
            yield() // to second child
            expect(4)
            throw TestException1()
        }
        val job2 = launch(supervisor + CoroutineName("job2")) {
            expect(3)
            throw TestException2()
        }
        joinAll(job1, job2)
        finish(5)
        assertTrue(job1.isFailed)
        assertTrue(job2.isFailed)
    }

    @Test
    fun testSupervisorScope() = runTest(
        unhandled = listOf(
            { it -> it is TestException1 },
            { it -> it is TestException2 }
        )
    ) {
        val result = supervisorScope {
            launch {
                throw TestException1()
            }
            launch {
                throw TestException2()
            }
            "OK"
        }
        assertEquals("OK", result)
    }

    private class TestException1 : Exception()
    private class TestException2 : Exception()
}