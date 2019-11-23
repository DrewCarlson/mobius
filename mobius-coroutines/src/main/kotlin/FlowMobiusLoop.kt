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

import com.spotify.mobius.MobiusLoop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@UseExperimental(ExperimentalCoroutinesApi::class)
internal class FlowMobiusLoop<M, E> internal constructor(
    private val loopFactory: MobiusLoop.Factory<M, E, *>,
    private val startModel: M
) : FlowTransformer<E, M> {

    override fun invoke(events: Flow<E>): Flow<M> =
        callbackFlow {
            val loop = loopFactory.startFrom(startModel)

            loop.observe { newModel -> offer(newModel) }

            events
                .onEach { event -> loop.dispatchEvent(event) }
                .catch { e -> throw UnrecoverableIncomingException(e) }
                .launchIn(this)

            awaitClose { loop.dispose() }
        }
}
