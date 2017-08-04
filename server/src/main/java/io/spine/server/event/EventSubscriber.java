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

package io.spine.server.event;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.reflect.EventSubscriberMethod;
import io.spine.server.tenant.EventOperation;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;
import io.spine.util.Logging;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * The abstract base for objects that can be subscribed to receive events from {@link EventBus}.
 *
 * <p>Objects may also receive events via {@link EventDispatcher}s that can be
 * registered with {@code EventBus}.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 * @see EventBus#register(MessageDispatcher)
 */
public abstract class EventSubscriber implements EventDispatcher<String> {

    /**
     * Cached set of the event classes this subscriber is subscribed to.
     */
    @Nullable
    private Set<EventClass> eventClasses;

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    /**
     * {@inheritDoc}
     *
     * @param envelope the envelope with the message
     * @return a one element set with the result of {@link #toString()}
     * as the identify of the subscriber, or empty set if dispatching failed
     */
    @Override
    public Set<String> dispatch(final EventEnvelope envelope) {
        final EventOperation op = new EventOperation(envelope.getOuterObject()) {
            @Override
            public void run() {
                handle(envelope.getMessage(), envelope.getEventContext());
            }
        };
        try {
            op.execute();
        } catch (RuntimeException exception) {
            onError(envelope, exception);
            return ImmutableSet.of();
        }
        return Identity.of(this);
    }

    /**
     * Logs the error into the subscriber {@linkplain #log() log}.
     *
     * @param envelope  the message which caused the error
     * @param exception the error
     */
    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        final MessageClass messageClass = envelope.getMessageClass();
        final String messageId = Stringifiers.toString(envelope.getId());
        final String errorMessage =
                format("Error handling event subscription (class: %s id: %s).",
                       messageClass, messageId);
        log().error(errorMessage, exception);
    }

    /**
     * Obtains the instance of logger associated with the class of the subscriber.
     */
    protected Logger log() {
        return loggerSupplier.get();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<EventClass> getMessageClasses() {
        if (eventClasses == null) {
            eventClasses = EventSubscriberMethod.inspect(getClass());
        }
        return eventClasses;
    }

    public void handle(Message eventMessage, EventContext context) {
        EventSubscriberMethod.invokeFor(this, eventMessage, context);
    }
}
