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
package io.spine.server.event.store;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.logging.Logging;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toSet;

/**
 * A store of all events in a Bounded Context.
 */
public final class EventStore implements AutoCloseable, Logging {

    private static final String TENANT_MISMATCH_ERROR_MSG =
            "Events, that target different tenants, cannot be stored in a single operation. " +
                    System.lineSeparator() +
                    "Observed tenants are: %s.";

    private final ERepository storage;
    private final Log log;

    /**
     * Constructs new instance taking arguments from the passed builder.
     */
    public EventStore() {
        super();
        this.storage = new ERepository();
        this.log = new Log(Logging.get(getClass()));
    }

    @Internal
    public void init(BoundedContext context) {
        context.register(storage);
    }

    /**
     * Appends the passed event to the history of events.
     *
     * @param event the record to append
     */
    public void append(Event event) {
        checkNotNull(event);
        TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                storage.store(event);
            }
        };
        op.execute();

        log.stored(event);
    }

    /**
     * Appends the passed events to the history of events.
     *
     * <p>If the passed {@link Iterable} is empty, no action is performed.
     *
     * <p>If the passed {@linkplain Event Events} belong to the different
     * {@linkplain TenantId tenants}, an {@link IllegalArgumentException} is thrown.
     *
     * @param events the events to append
     */
    public void appendAll(Iterable<Event> events) {
        checkNotNull(events);
        ImmutableList<Event> eventList =
                Streams.stream(events)
                       .filter(Objects::nonNull)
                       .collect(toImmutableList());
        if (eventList.isEmpty()) {
            return;
        }
        Event event = eventList.get(0);
        TenantAwareOperation op = new EventOperation(event) {
            @Override
            public void run() {
                if (isTenantSet()) { // If multitenant context
                    ensureSameTenant(eventList);
                }
                storage.store(eventList);
            }
        };
        op.execute();

        log.stored(events);
    }

    private static void ensureSameTenant(ImmutableList<Event> events) {
        checkNotNull(events);
        Set<TenantId> tenants = events.stream()
                                      .map(Event::tenant)
                                      .collect(toSet());
        checkArgument(tenants.size() == 1, TENANT_MISMATCH_ERROR_MSG, tenants);
    }

    /**
     * Creates the stream with events matching the passed query.
     *
     * @param request          the query with filtering parameters for the event history
     * @param responseObserver observer for the resulting stream
     */
    public void read(EventStreamQuery request, StreamObserver<Event> responseObserver) {
        checkNotNull(request);
        checkNotNull(responseObserver);

        log.readingStart(request, responseObserver);

        Iterator<Event> eventRecords = storage.iterator(request);
        while (eventRecords.hasNext()) {
            Event event = eventRecords.next();
            responseObserver.onNext(event);
        }
        responseObserver.onCompleted();

        log.readingComplete(responseObserver);
    }

    /**
     * Closes the underlying storage.
     */
    @Override
    public void close() {
        storage.close();
    }

    /**
     * Tells if the store is open.
     */
    public boolean isOpen() {
        return storage.isOpen();
    }
}
