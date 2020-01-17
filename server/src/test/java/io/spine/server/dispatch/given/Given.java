/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.dispatch.given;

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.RejectionEventContext;
import io.spine.server.dispatch.DispatchOutcomeHandlerTest;
import io.spine.server.dispatch.given.command.CreateDispatch;
import io.spine.server.dispatch.given.event.DispatchCreated;
import io.spine.server.dispatch.given.rejection.DispatchRejections;
import io.spine.testing.TestValues;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import static io.spine.protobuf.TypeConverter.toAny;

public final class Given {

    private static final Id ID = Id.newBuilder()
                                   .setId(TestValues.randomString())
                                   .build();

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(toAny(Given.class.getSimpleName()),
                                         DispatchOutcomeHandlerTest.class);

    private static final CommandFactory commandFactory =
            new TestActorRequestFactory(DispatchOutcomeHandlerTest.class).command();

    /** Prevent instantiation of this utility class. */
    private Given() {
    }

    public static Event rejectionEvent() {
        RejectionEventContext rejectionContext = RejectionEventContext
                .newBuilder()
                .setStacktrace("at package.name.Class.method(Class.java:42)")
                .build();
        return eventFactory.createRejectionEvent(cannotCreateDispatch(), null, rejectionContext);
    }

    public static Event event() {
        return eventFactory.createEvent(dispatchCreated());
    }

    public static Command command() {
        return commandFactory.create(createDispatch());
    }

    private static RejectionMessage cannotCreateDispatch() {
        return DispatchRejections.CannotCreateDispatch
                .newBuilder()
                .setId(ID)
                .build();
    }

    private static EventMessage dispatchCreated() {
        return DispatchCreated
                .newBuilder()
                .setId(ID)
                .build();
    }

    private static CommandMessage createDispatch() {
        return CreateDispatch
                .newBuilder()
                .setId(ID)
                .build();
    }
}
