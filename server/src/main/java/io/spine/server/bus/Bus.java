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

package io.spine.server.bus;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.FailureThrowable;
import io.spine.core.IsSent;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.validate.Validate.isNotDefault;
import static java.util.Collections.singleton;

/**
 * Abstract base for buses.
 *
 * @param <T> the type of outer objects (containing messages of interest) that are posted the bus
 * @param <E> the type of envelopes for outer objects used by this bus
 * @param <C> the type of message class
 * @param <D> the type of dispatches used by this bus
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
public abstract class Bus<T extends Message,
                          E extends MessageEnvelope<T>,
                          C extends MessageClass,
                          D extends MessageDispatcher<C, E>> implements AutoCloseable {

    private final Function<T, E> messageConverter = new MessageToEnvelope();

    @Nullable
    private DispatcherRegistry<C, D> registry;

    /**
     * Registers the passed dispatcher.
     *
     * @param dispatcher the dispatcher to register
     * @throws IllegalArgumentException if the set of message classes
     *                                  {@linkplain MessageDispatcher#getMessageClasses() exposed}
     *                                  by the dispatcher is empty
     */
    public void register(D dispatcher) {
        registry().register(checkNotNull(dispatcher));
    }

    /**
     * Unregisters dispatching for message classes of the passed dispatcher.
     *
     * @param dispatcher the dispatcher to unregister
     */
    public void unregister(D dispatcher) {
        registry().unregister(checkNotNull(dispatcher));
    }

    /**
     * Posts the message to the bus.
     *
     * @param message  the message to post
     * @param observer the observer to receive outcome of the operation
     * @see #post(Iterable, StreamObserver) for posing multiple messages at once
     */
    public final void post(T message, StreamObserver<IsSent> observer) {
        checkNotNull(message);
        checkNotNull(observer);
        checkArgument(isNotDefault(message));

        post(singleton(message), observer);
    }

    /**
     * Posts the given messages to the bus.
     *
     * <p>The {@linkplain StreamObserver observer} serves to notify the consumer about the result
     * of the call. The {@link StreamObserver#onNext StreamObserver.onNext()} is called for each
     * message posted to the bus.
     *
     * <p>In case the message is accepted by the bus, {@linkplain IsSent IsSent} with the
     * {@link io.spine.core.Status.StatusCase#OK OK} status is passed to the observer.
     *
     * <p>If the message cannot be sent due to some issues, a corresponding
     * {@link io.spine.base.Error Error} status is passed in {@code IsSent} instance.
     *
     * <p>Depending on the underlying {@link MessageDispatcher}, a message which causes a business
     * {@link io.spine.core.Failure} may result ether a {@link io.spine.core.Failure} status or
     * an {@link io.spine.core.Status.StatusCase#OK OK} status {@link IsSent} instance. Usually,
     * the {@link io.spine.core.Failure} status may only pop up if the {@link MessageDispatcher}
     * processes the message sequentially and throws
     * the {@linkplain FailureThrowable FailureThrowables} (wrapped in a
     * {@link RuntimeException}) instead of handling them. Otherwise, the {@code OK} status should
     * be expected.
     *
     * <p>Note that {@linkplain StreamObserver#onError StreamObserver.onError()} is never called
     * for the passed observer, since errors are propagated as statuses of {@code IsSent} response.
     *
     * @param messages the messages to post
     * @param observer the observer to receive outcome of the operation
     */
    public final void post(Iterable<T> messages, StreamObserver<IsSent> observer) {
        checkNotNull(messages);
        checkNotNull(observer);

        final Iterable<T> filteredMessages = filter(messages, observer);
        if (!isEmpty(filteredMessages)) {
            store(messages);
            final Iterable<E> envelopes = transform(filteredMessages, toEnvelope());
            doPost(envelopes, observer);
        }
        observer.onCompleted();
    }

    /**
     * Handles the message, for which there is no dispatchers registered in the registry.
     *
     * @param message the message that has no target dispatchers, packed into an envelope
     */
    public abstract void handleDeadMessage(E message);

    /**
     * Retrieves the ID of the given {@link MessageEnvelope}.
     */
    protected abstract Message getId(E envelope);

    /**
     * Obtains the dispatcher registry.
     */
    protected DispatcherRegistry<C, D> registry() {
        if (registry == null) {
            registry = createRegistry();
        }
        return registry;
    }

    /**
     * Factory method for creating an instance of the registry for
     * dispatchers of the bus.
     */
    protected abstract DispatcherRegistry<C, D> createRegistry();

    /**
     * Filters the given messages.
     *
     * <p>Each message goes through the filter chain, specific to the {@code Bus} implementation.
     *
     * <p>If a message passes the filtering, it is included into the resulting {@link Iterable};
     * otherwise, {@linkplain StreamObserver#onNext StreamObserver.onNext()} is called for that message.
     *
     * <p>Any filter in the filter chain may process the message by itself. In this case an observer
     * is notified by the filter directly.
     *
     * @param messages the message to filter
     * @param observer the observer to receive the negative outcome of the operation
     * @return the message itself if it passes the filtering or
     * {@link Optional#absent() Optional.absent()} otherwise
     */
    private Iterable<T> filter(Iterable<T> messages, StreamObserver<IsSent> observer) {
        checkNotNull(messages);
        checkNotNull(observer);
        final Collection<T> result = newLinkedList();
        for (T message : messages) {
            final Optional<IsSent> response = preProcess(toEnvelope(message));
            if (response.isPresent()) {
                observer.onNext(response.get());
            } else {
                result.add(message);
            }
        }
        return result;
    }

    /**
     * Pre-processes the given message.
     *
     * <p>The pre-processing may include validation.
     *
     * <p>If the given message is completely processed and should not be passed to the dispatchers
     * via {@link #doPost doPost} method, the returned {@link Optional} contains a value with either
     * status.
     *
     * <p>If the message should be passed to the dispatchers via {@link #doPost doPost}, the result
     * of this method is {@link Optional#absent() Optional.absent()}.
     *
     * @param message the {@linkplain MessageEnvelope message envelope} to pre-process
     * @return the result of message processing by this bus if any, or
     * {@link Optional#absent() Optional.absent()} otherwise
     */
    protected abstract Optional<IsSent> preProcess(E message);

    /**
     * Packs the given message of type {@code T} into an envelope of type {@code E}.
     *
     * @param message the message to pack
     * @return new envelope with the given message inside
     */
    protected abstract E toEnvelope(T message);

    /**
     * Posts the given envelope to the bus.
     *
     * <p>Finds and invokes the {@linkplain MessageDispatcher MessageDispatcher(s)} for the given
     * message.
     *
     * <p>This method assumes that the given message has passed the filtering.
     *
     * @return the result of mailing with the Message ID and:
     *         <ul>
     *             <li>{@link io.spine.core.Status.StatusCase#OK OK} status if the message has been
     *                 passed to the dispatcher;
     *             <li>{@link io.spine.core.Failure Failure} status, if a {@code Failure} has
     *                 happened during the message handling (if applicable);
     *             <li>{@link io.spine.base.Error Error} status if a {@link Throwable}, which is not
     *                 a {@link FailureThrowable FailureThrowable}, has been thrown
     *                 during the message posting.
     *         </ul>
     * @see #post(Message, StreamObserver) for the public API
     */
    protected abstract IsSent doPost(E envelope);

    /**
     * Posts each of the given envelopes into the bus and notifies the given observer.
     *
     * @param envelopes the envelopes to post
     * @param observer  the observer to be notified of the operation result
     * @see #doPost(MessageEnvelope)
     */
    private void doPost(Iterable<E> envelopes, StreamObserver<IsSent> observer) {
        for (E message : envelopes) {
            final IsSent result = doPost(message);
            observer.onNext(result);
        }
    }

    /**
     * Stores the given messages into the underlying storage.
     *
     * @param messages the messages to store
     */
    protected abstract void store(Iterable<T> messages);

    /**
     * @return a {@link Function} converting the messages into the envelopes of the specified
     * type
     */
    private Function<T, E> toEnvelope() {
        return messageConverter;
    }

    /**
     * A function creating the instances of {@link MessageEnvelope} from the given message.
     */
    private class MessageToEnvelope implements Function<T, E> {

        @Override
        public E apply(@Nullable T message) {
            checkNotNull(message);
            final E result = toEnvelope(message);
            return result;
        }
    }
}
