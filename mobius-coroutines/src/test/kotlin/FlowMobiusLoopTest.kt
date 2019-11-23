/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius.flow

import com.spotify.mobius.Connection
import com.spotify.mobius.Mobius
import com.spotify.mobius.Next.next
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.lang.Exception

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FlowMobiusLoopTest {

    private lateinit var loop: FlowMobiusLoop<String, Int>

    @Before
    fun setUp() {
        val factory = Mobius.loop<String, Int, Boolean>(
            { model, event -> next(model + event.toString()) },
            {
                object : Connection<Boolean> {
                    override fun accept(value: Boolean) = Unit
                    override fun dispose() = Unit
                }
            }
        )

        loop = FlowMobiusLoop(factory, "")
    }

    @Test
    fun shouldPropagateIncomingErrorsAsUnrecoverable() = runBlockingTest {
        val input = Channel<Int>()

        val result = async {
            loop(input.consumeAsFlow()).toList()
        }

        input.close(RuntimeException("expected"))

        try {
            result.await()
            fail()
        } catch (e: Exception) {
            assertTrue(e is UnrecoverableIncomingException)
            assertEquals("expected", e.cause?.message)
        }
    }
}
