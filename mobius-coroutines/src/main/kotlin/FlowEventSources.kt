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

import com.spotify.mobius.EventSource
import com.spotify.mobius.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Create an [EventSource] that emits each [E] from this [Flow].
 *
 * Collection of the flow will be launched in [scope] when
 * [EventSource.subscribe] is called and the job cancelled when
 * on [Disposable.dispose].
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <E> Flow<E>.toEventSource(
    scope: CoroutineScope
): EventSource<E> =
    EventSource { output ->
        val job = onEach { event ->
            output.accept(event)
        }.launchIn(scope)

        Disposable { job.cancel() }
    }

/**
 * Create a [Flow] that emits each [E] from this [EventSource].
 *
 * [EventSource.subscribe] is called when collection begins and
 * [Disposable.dispose] is called when the [Flow] is cancelled.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <E> EventSource<E>.toFlow(): Flow<E> =
    callbackFlow {
        val disposable = subscribe { event -> offer(event) }

        awaitClose { disposable.dispose() }
    }
