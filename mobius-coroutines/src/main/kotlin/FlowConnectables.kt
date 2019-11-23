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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

/**
 * Constructs a [Connectable] that applies [transform] to
 * map a [Flow] of [I] into a [Flow] of [O].
 */
@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun <I, O> flowConnectable(
    transform: FlowTransformer<I, O>
) = Connectable<I, O> { consumer ->
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val inputChannel = BroadcastChannel<I>(BUFFERED)
    inputChannel.asFlow()
        .run(transform)
        .onEach { output -> consumer.accept(output) }
        .launchIn(scope)
    object : Connection<I> {
        override fun accept(value: I) {
            inputChannel.offer(value)
        }

        override fun dispose() {
            scope.cancel()
        }
    }
}

/**
 * Transforms the [FlowTransformer] into a [Connectable].
 */
fun <I, O> FlowTransformer<I, O>.asConnectable(): Connectable<I, O> =
    flowConnectable(this)

/**
 * Transforms a [Flow] of [I] into a [Flow] of [O] using
 * the provided [Connectable] [connectable].
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <I, O> Flow<I>.transform(
    connectable: Connectable<I, O>
): Flow<O> = callbackFlow {
    val connection = connectable.connect { output ->
        offer(output)
    }
    onEach { input ->
        connection.accept(input)
    }.onCompletion {
        close()
    }.launchIn(this)

    awaitClose {
        connection.dispose()
    }
}
