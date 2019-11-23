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

import com.spotify.mobius.test.RecordingConsumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertTrue
import org.junit.Test

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FlowEventSourcesTest {

    @Test
    fun eventsAreForwardedInOrder() = runBlockingTest {
        val source = flowOf(1, 2, 3).toEventSource(this)
        val consumer = RecordingConsumer<Int>()

        source.subscribe(consumer)

        consumer.waitForChange(50)
        consumer.assertValues(1, 2, 3)
    }

    @Test
    fun disposePreventsFurtherEvents() = runBlockingTest {
        val channel = Channel<Int>()
        val source = channel.consumeAsFlow().toEventSource(this)
        val consumer = RecordingConsumer<Int>()

        val subscription = source.subscribe(consumer)

        channel.send(1)
        channel.send(2)
        subscription.dispose()

        consumer.waitForChange(50)
        consumer.assertValues(1, 2)

        assertTrue(channel.isClosedForSend)
    }
}
