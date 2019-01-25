/*
 * Copyright 2019, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.command.model;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.CommandEnvelope;
import io.spine.server.EventProducer;
import io.spine.server.command.CommandHandler;
import io.spine.server.model.EventsResult;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.procman.ProcessManager;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandHandlerMethod
        extends CommandAcceptingMethod<CommandHandler, CommandHandlerMethod.Result> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method   command handler method
     * @param emittedMessages
     */
    CommandHandlerMethod(Method method, ParameterSpec<CommandEnvelope> params,
                         ImmutableSet<Class<? extends Message>> emittedMessages) {
        super(method, params, emittedMessages);
    }

    /**
     * Transforms the passed raw method output into a list of event messages.
     */
    @Override
    protected Result toResult(CommandHandler target, Object rawMethodOutput) {
        return new Result(target, rawMethodOutput);
    }

    /**
     * The result of a command handler method execution.
     */
    public static final class Result extends EventsResult {

        private Result(EventProducer producer, Object rawMethodResult) {
            super(producer, rawMethodResult);
            List<EventMessage> eventMessages = toMessages(rawMethodResult);
            List<EventMessage> filtered = filterIgnored(eventMessages);
            ensureNotEmptyIfNotProcessManager(filtered, producer);
            setMessages(filtered);
        }

        /**
         * Ensures that a command handler method produces one or more event messages.
         *
         * <p>The only allowed exception to this are {@link ProcessManager} instances returning
         * empty from command handlers.
         *
         * @param eventMessages  the events produced as the result of the command handling
         * @param target         the target on which the method was executed
         * @throws IllegalStateException if the command handling method did not produce any events
         */
        private static void ensureNotEmptyIfNotProcessManager(List<? extends Message> eventMessages,
                                                              Object target) {

            //TODO:2018-07-25:dmytro.kuzmin: Prohibit returning empty events from `ProcessManager`
            //todo    in favor of "Expect<...>" construction.
            //todo    See https://github.com/SpineEventEngine/core-java/issues/790.
            boolean empty = eventMessages.isEmpty();
            if (empty) {
                boolean procman = target instanceof ProcessManager;
                checkState(procman, "Command handling method did not produce events");
            }
        }
    }
}
