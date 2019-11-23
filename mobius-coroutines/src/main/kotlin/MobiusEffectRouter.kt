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

import com.spotify.mobius.flow.internal.share
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map

@UseExperimental(FlowPreview::class)
class MobiusEffectRouter<F : Any, E>(
    private val effectClasses: Set<Class<*>>,
    private val effectPerformers: List<FlowTransformer<F, E>>
) : FlowTransformer<F, E> {

    private val unhandledEffectHandler =
        flowTransformer<F, E> { effects ->
            effects
                .filter { effect ->
                    effectClasses
                        .none { effectClass ->
                            effectClass.isAssignableFrom(effect::class.java)
                        }
                }
                .map { effect -> throw UnknownEffectException(effect) }
        }

    override fun invoke(effects: Flow<F>): Flow<E> {
        val sharedEffects = effects.share()

        return (effectPerformers + unhandledEffectHandler)
            .map { transform -> transform(sharedEffects) }
            .run {
                asFlow()
                    .flattenMerge(concurrency = size)
            }
    }
}

data class UnknownEffectException(
    val effect: Any
) : RuntimeException() {
    override val message = effect.toString()
}
