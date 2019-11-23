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

import com.spotify.mobius.runners.WorkRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** A [WorkRunner] that launches work on a [CoroutineDispatcher]. */
class DispatcherWorkRunner(
    dispatcher: CoroutineDispatcher
) : WorkRunner {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    override fun post(runnable: Runnable) {
        scope.launch { runnable.run() }
    }

    override fun dispose() {
        scope.cancel()
    }
}
