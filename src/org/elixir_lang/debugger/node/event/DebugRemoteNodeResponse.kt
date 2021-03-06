/*
 * Copyright 2012-2014 Sergey Ignatov
 * Copyright 2017-2018 Luke Imhoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elixir_lang.debugger.node.event

import com.ericsson.otp.erlang.OtpErlangAtom
import com.ericsson.otp.erlang.OtpErlangTuple
import com.intellij.openapi.diagnostic.Logger
import org.elixir_lang.beam.term.inspect
import org.elixir_lang.debugger.Node
import org.elixir_lang.debugger.node.ErrorReason
import org.elixir_lang.debugger.node.Event
import org.elixir_lang.debugger.node.OK
import org.elixir_lang.debugger.node.OKErrorReason

class DebugRemoteNodeResponse(val node: String, private val okErrorReason: OKErrorReason) : Event() {
    override fun process(node: Node, eventListener: Listener) =
        when (okErrorReason) {
            OK -> Unit
            is ErrorReason -> eventListener.failedToDebugRemoteNode(this.node, okErrorReason.reason)
        }

    companion object {
        // {node, :ok | {:error, reason}}
        const val ARITY = 2
        const val NAME = "debug_remote_node_response"

        private val LOGGER = Logger.getInstance(DebugRemoteNodeResponse::class.java)

        fun from(tuple: OtpErlangTuple): DebugRemoteNodeResponse? {
            val arity = tuple.arity()

            return if (arity == ARITY) {
                node(tuple)?.let { node ->
                    OKErrorReason.from(tuple.elementAt(1))?.let { okErrorReason ->
                        DebugRemoteNodeResponse(node, okErrorReason)
                    }
                }
            } else {
                LOGGER.error(":$NAME message (${inspect(tuple)}) arity ($arity) is not $ARITY")

                null
            }
        }

        private fun node(tuple: OtpErlangTuple): String? {
            val node = tuple.elementAt(0)

            return when (node) {
                is OtpErlangAtom -> node.atomValue()
                else -> {
                    LOGGER.error("Node (${inspect(node)}) is not an OtpErlangAtom")

                    null
                }
            }
        }
    }
}
