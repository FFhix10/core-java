/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.integration;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.server.event.EventSubscriber;
import io.spine.server.reflect.EventSubscriberMethod;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;
import io.spine.util.Logging;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Events.isExternal;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * An internal wrapper class, which exposes a {@link EventSubscriber}
 * as an {@link ExternalMessageDispatcher}.
 *
 * <p>Allows to register {@code EventSubscriber}s as dispatchers of
 * {@code IntegrationBus}.
 *
 * @author Alex Tymchenko
 */
class ExternalEventSubscriber implements ExternalMessageDispatcher<String> {

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    private final EventSubscriber delegate;

    /**
     * Set of the external event classes this subscriber is subscribed to.
     */
    private final Set<EventClass> eventClasses;

    ExternalEventSubscriber(EventSubscriber delegate) {
        this.delegate = delegate;
        this.eventClasses = EventSubscriberMethod.inspectExternal(delegate.getClass());
    }

    @Override
    public Set<MessageClass> getMessageClasses() {
        return ImmutableSet.<MessageClass>copyOf(eventClasses);
    }

    @Override
    public Set<String> dispatch(ExternalMessageEnvelope envelope) {
        final Message outerObject = envelope.getOuterObject();
        if (!(outerObject instanceof Event)) {
            throw newIllegalStateException("Unexpected object %s while dispatching " +
                                                   "the external event to the event subscriber.",
                                           Stringifiers.toString(outerObject));
        }
        final Event event = (Event) outerObject;
        checkArgument(isExternal(event.getContext()),
                      "External event expected, but got %s", Stringifiers.toString(event));
        return delegate.dispatch(EventEnvelope.of(event));
    }

    @Override
    public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);

        final MessageClass messageClass = envelope.getMessageClass();
        final String messageId = Stringifiers.toString(envelope.getId());
        final String errorMessage =
                format("Error handling external event subscription (class: %s id: %s).",
                       messageClass, messageId);
        log().error(errorMessage, exception);
    }

    private Logger log() {
        return loggerSupplier.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExternalEventSubscriber that = (ExternalEventSubscriber) o;
        return Objects.equals(delegate, that.delegate) &&
                Objects.equals(eventClasses, that.eventClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, eventClasses);
    }
}
