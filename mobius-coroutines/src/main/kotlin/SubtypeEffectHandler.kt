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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.invoke

fun <F : Any, E> subtypeEffectHandler(
    block: SubtypeEffectHandlerBuilder<F, E>.() -> Unit
): FlowTransformer<F, E> =
    SubtypeEffectHandlerBuilder<F, E>()
        .apply(block)
        .build()

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SubtypeEffectHandlerBuilder<F : Any, E> {
    private val effectPerformerMap = hashMapOf<Class<*>, FlowTransformer<F, E>>()

    inline fun <reified G : F> addTransformer(
        noinline effectHandler: FlowTransformer<G, E>
    ) = addTransformer(G::class.java, effectHandler)

    inline fun <reified G : F> addFunction(
        noinline effectHandler: suspend (effect: G) -> E
    ) = addFunction(G::class.java, effectHandler)

    inline fun <reified G : F> addConsumer(
        noinline effectHandler: suspend (effect: G) -> Unit
    ) = addConsumer(G::class.java, effectHandler)

    inline fun <reified G : F> addAction(
        noinline effectHandler: suspend () -> Unit
    ) = addAction(G::class.java, effectHandler)

    inline fun <reified G : F> addFunctionSync(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline effectHandler: (effect: G) -> E
    ) = addFunction(G::class.java) { effect ->
        dispatcher { effectHandler(effect) }
    }

    inline fun <reified G : F> addConsumerSync(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline effectHandler: (effect: G) -> Unit
    ) = addConsumer(G::class.java) { effect ->
        dispatcher { effectHandler(effect) }
    }

    inline fun <reified G : F> addActionSync(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline effectHandler: () -> Unit
    ) = addAction(G::class.java) {
        dispatcher { effectHandler() }
    }

    fun <G : F> addTransformer(
        effectClass: Class<G>,
        effectHandler: FlowTransformer<G, E>
    ): SubtypeEffectHandlerBuilder<F, E> {
        effectPerformerMap
            .keys
            .forEach { cls ->
                val assignable = cls.isAssignableFrom(effectClass) ||
                    effectClass.isAssignableFrom(cls)
                check(!assignable) {
                    "Sub-type effect handler already registered for '${effectClass.name}'"
                }
            }
        effectPerformerMap[effectClass] = { effects ->
            effects
                .filter { effect -> effectClass.isInstance(effect) }
                .map { effect -> effectClass.cast(effect) }
                .run(effectHandler)
            // TODO: .catch unhandled exceptions
        }

        return this
    }

    fun <G : F> addFunction(
        effectClass: Class<G>,
        effectHandler: suspend (effect: G) -> E
    ) = addTransformer(effectClass) { effects ->
        effects.map { effect -> effectHandler(effect) }
    }

    fun <G : F> addConsumer(
        effectClass: Class<G>,
        effectHandler: suspend (effect: G) -> Unit
    ) = addTransformer(effectClass) { effects ->
        effects.transform { effect -> effectHandler(effect) }
    }

    fun <G : F> addAction(
        effectClass: Class<G>,
        effectHandler: suspend () -> Unit
    ) = addTransformer(effectClass) { effects ->
        effects.transform { effectHandler() }
    }

    fun build(): FlowTransformer<F, E> =
        MobiusEffectRouter(
            effectClasses = effectPerformerMap.keys.toSet(),
            effectPerformers = effectPerformerMap.values.toList()
        )
}
