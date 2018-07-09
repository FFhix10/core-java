/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.projection;

import com.google.protobuf.Message;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.EventSubscriptionTest;

import java.util.List;

import static io.spine.server.projection.ProjectionEventDispatcher.dispatch;
import static java.util.Collections.emptyList;

/**
 * The implementation base for testing projection event subscriptions.
 *
 * @author Dmytro Dashenkov
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("TestOnlyProblems")
public abstract class ProjectionTest<M extends Message,
                                     I,
                                     S extends Message,
                                     P extends Projection<I, S, ?>>
        extends EventSubscriptionTest<M, I, S, P> {

    @Override
    protected List<? extends Message> dispatchTo(P entity) {
        Event sourceEvent = createEvent();
        EventContext context = sourceEvent.getContext()
                                          .toBuilder()
                                          .setEnrichment(enrichment())
                                          .build();
        Event enrichedEvent = sourceEvent.toBuilder()
                                         .setContext(context)
                                         .build();

        dispatch(entity, enrichedEvent);
        return emptyList();
    }

    protected Enrichment enrichment() {
        return Enrichment.getDefaultInstance();
    }
}
