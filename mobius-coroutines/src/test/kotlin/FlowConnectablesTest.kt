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

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FlowConnectablesTest {

    private lateinit var input: Channel<String>
    private lateinit var connectable: Connectable<String, Int>

    @Before
    fun setUp() {
        input = Channel(UNLIMITED)
        connectable = Connectable { output ->
            object : Connection<String> {
                override fun accept(value: String) {
                    if (value == "crash") {
                        throw RuntimeException("crashing!")
                    }

                    output.accept(value.length)
                }

                override fun dispose() = Unit
            }
        }
    }

    @Test
    fun shouldEmitTransformedItems() = runBlockingTest {
        val results = async {
            input.consumeAsFlow()
                .transform(connectable)
                .toList()
        }

        input.send(".")
        input.send("..")
        input.send("...")
        input.close()

        assertEquals("1, 2, 3", results.await().joinToString())
    }

    @Test
    fun shouldPropagateCompletion() = runBlockingTest {
        val job = input.consumeAsFlow()
            .transform(connectable)
            .launchIn(this)

        assertFalse(job.isCompleted)

        input.send("hi")
        input.close()

        assertTrue(job.isCompleted)
    }

    @Test
    fun shouldPropagateErrorsFromConnectable() = runBlockingTest {
        val output = async {
            input.consumeAsFlow()
                .transform(connectable)
                .toList()
        }

        input.send("crash")

        try {
            output.await()
            fail()
        } catch (e: Exception) {
            assertEquals("crashing!", e.message)
        }
    }

    @Test
    fun shouldPropagateErrorsFromUpstream() = runBlockingTest {
        val output = async {
            input.consumeAsFlow()
                .transform(connectable)
                .toList()
        }

        input.close(RuntimeException("expected"))

        try {
            output.await()
            fail()
        } catch (e: Exception) {
            assertEquals("expected", e.message)
        }
    }
}
