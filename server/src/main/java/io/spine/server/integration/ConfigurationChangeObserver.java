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

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextId;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An observer, which reacts to the configuration update messages sent by
 * external entities (such as {@code IntegrationBus}es of other bounded contexts).
 *
 * @author Alex Tymchenko
 */
class ConfigurationChangeObserver extends AbstractChannelObserver implements AutoCloseable {

    private final BoundedContextId boundedContextId;
    private final Function<Class<? extends Message>, BusAdapter<?, ?>> adapterByClass;

    /**
     * Current set of message type URLs, requested by other parties via sending the
     * {@linkplain RequestForExternalMessages configuration messages}, mapped to IDs of their origin
     * bounded contexts.
     */
    private final Multimap<ExternalMessageType, BoundedContextId> requestedTypes =
            HashMultimap.create();

    ConfigurationChangeObserver(BoundedContextId boundedContextId,
                                Function<Class<? extends Message>,
                                        BusAdapter<?, ?>> adapterByClass) {
        super(boundedContextId, RequestForExternalMessages.class);
        this.boundedContextId = boundedContextId;
        this.adapterByClass = adapterByClass;
    }

    @Override
    public void handle(ExternalMessage value) {
        final RequestForExternalMessages request = AnyPacker.unpack(value.getOriginalMessage());

        final BoundedContextId originBoundedContextId = value.getBoundedContextId();
        addNewSubscriptions(request.getRequestedMessageTypesList(), originBoundedContextId);
        clearStaleSubscriptions(request.getRequestedMessageTypesList(), originBoundedContextId);
    }

    private void addNewSubscriptions(Iterable<ExternalMessageType> types,
                                     BoundedContextId originBoundedContextId) {
        for (ExternalMessageType newType : types) {
            final Collection<BoundedContextId> contextsWithSameRequest =
                    requestedTypes.get(newType);
            if (contextsWithSameRequest.isEmpty()) {

                // This item has is not requested by anyone at the moment.
                // Let's create a subscription.

                registerInAdapter(newType);
            }

            requestedTypes.put(newType, originBoundedContextId);
        }
    }

    private void registerInAdapter(ExternalMessageType newType) {
        final Class<Message> wrapperCls = asClassOfMsg(newType.getWrapperTypeUrl());
        final Class<Message> messageCls = asClassOfMsg(newType.getMessageTypeUrl());
        final BusAdapter<?, ?> adapter = getAdapter(wrapperCls);
        adapter.register(messageCls);
    }

    private BusAdapter<?, ?> getAdapter(Class<Message> javaClass) {
        final BusAdapter<?, ?> adapter = adapterByClass.apply(javaClass);
        return checkNotNull(adapter);
    }

    private void clearStaleSubscriptions(Collection<ExternalMessageType> types,
                                         BoundedContextId originBoundedContextId) {

        final Set<ExternalMessageType> toRemove = findStale(types, originBoundedContextId);

        for (ExternalMessageType itemForRemoval : toRemove) {
            final boolean wereNonEmpty = !requestedTypes.get(itemForRemoval)
                                                        .isEmpty();
            requestedTypes.remove(itemForRemoval, originBoundedContextId);
            final boolean emptyNow = requestedTypes.get(itemForRemoval)
                                                   .isEmpty();

            if (wereNonEmpty && emptyNow) {
                unregisterInAdapter(itemForRemoval);
            }
        }
    }

    private void unregisterInAdapter(ExternalMessageType itemForRemoval) {
        // It's now the time to remove the local bus subscription.
        final Class<Message> wrapperCls = asClassOfMsg(itemForRemoval.getWrapperTypeUrl());
        final Class<Message> messageCls = asClassOfMsg(itemForRemoval.getMessageTypeUrl());
        final BusAdapter<?, ?> adapter = getAdapter(wrapperCls);
        adapter.unregister(messageCls);
    }

    private Set<ExternalMessageType> findStale(Collection<ExternalMessageType> types,
                                               BoundedContextId originBoundedContextId) {
        final ImmutableSet.Builder<ExternalMessageType> result = ImmutableSet.builder();

        for (ExternalMessageType previouslyRequestedType : requestedTypes.keySet()) {
            final Collection<BoundedContextId> contextsThatRequested =
                    requestedTypes.get(previouslyRequestedType);

            if (contextsThatRequested.contains(originBoundedContextId) &&
                    !types.contains(previouslyRequestedType)) {

                // The `previouslyRequestedType` item is no longer requested
                // by the bounded context with `originBoundedContextId` ID.

                result.add(previouslyRequestedType);
            }
        }
        return result.build();
    }

    @Override
    public String toString() {
        return "Integration bus observer of `RequestedMessageTypes`; " +
                "Bounded Context ID = " + boundedContextId.getValue();
    }

    private static Class<Message> asClassOfMsg(String classStr) {
        final TypeUrl typeUrl = TypeUrl.parse(classStr);
        return typeUrl.getJavaClass();
    }

    /**
     * Removes all the current subscriptions from the local buses.
     */
    @Override
    public void close() throws Exception {
        for (ExternalMessageType currentlyRequestedMessage : requestedTypes.keySet()) {
            unregisterInAdapter(currentlyRequestedMessage);
        }
        requestedTypes.clear();
    }
}
