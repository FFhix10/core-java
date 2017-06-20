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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.annotation.Subscribe;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.envelope.EventEnvelope;
import io.spine.io.StreamObservers;
import io.spine.io.StreamObservers.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.event.enrich.EventEnricher;
import io.spine.server.storage.StorageFactory;
import io.spine.test.EventTests;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.type.EventClass;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class EventBusShould {

    private EventBus eventBus;
    private EventBus eventBusWithPosponedExecution;
    private MemoizingObserver<Response> responseObserver;
    private PostponedDispatcherEventDelivery postponedDispatcherDelivery;
    private Executor delegateDispatcherExecutor;
    private StorageFactory storageFactory;

    @Before
    public void setUp() {
        setUp(null);
    }

    private void setUp(@Nullable EventEnricher enricher) {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(true)
                                                .build();
        this.storageFactory = bc.getStorageFactory();
        /**
         * Cannot use {@link com.google.common.util.concurrent.MoreExecutors#directExecutor()
         * MoreExecutors.directExecutor()} because it's impossible to spy on {@code final} classes.
         */
        this.delegateDispatcherExecutor = spy(directExecutor());
        this.postponedDispatcherDelivery =
                new PostponedDispatcherEventDelivery(delegateDispatcherExecutor);
        buildEventBus(enricher);
        buildEventBusWithPostponedExecution(enricher);
        this.responseObserver = StreamObservers.memoizingObserver();
    }

    @SuppressWarnings("MethodMayBeStatic")   /* it cannot, as its result is used in {@code org.mockito.Mockito.spy() */
    private Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private void buildEventBusWithPostponedExecution(@Nullable EventEnricher enricher) {
        final EventBus.Builder busBuilder =
                EventBus.newBuilder()
                        .setStorageFactory(storageFactory)
                        .setDispatcherEventDelivery(postponedDispatcherDelivery);

        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        this.eventBusWithPosponedExecution = busBuilder.build();
    }

    private void buildEventBus(@Nullable EventEnricher enricher) {
        final EventBus.Builder busBuilder = EventBus.newBuilder()
                                                    .setStorageFactory(storageFactory);
        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        this.eventBus = busBuilder.build();
    }

    @Test
    public void have_builder() {
        assertNotNull(EventBus.newBuilder());
    }

    @Test
    public void return_associated_EventStore() {
        final EventStore eventStore = mock(EventStore.class);
        final EventBus result = EventBus.newBuilder()
                                        .setEventStore(eventStore)
                                        .build();
        assertEquals(eventStore, result.getEventStore());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        // Pass just String instance.
        eventBus.register(new EventSubscriber() {
        });
    }

    @Test
    public void register_event_subscriber() {
        final EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
        final EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();

        eventBus.register(subscriberOne);
        eventBus.register(subscriberTwo);

        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        assertTrue(eventBus.hasDispatchers(eventClass));

        final Collection<EventDispatcher> dispatchers = eventBus.getDispatchers(eventClass);
        assertTrue(dispatchers.contains(subscriberOne));
        assertTrue(dispatchers.contains(subscriberTwo));
    }

    @Test
    public void unregister_subscribers() {
        final EventSubscriber subscriberOne = new ProjectCreatedSubscriber();
        final EventSubscriber subscriberTwo = new ProjectCreatedSubscriber();
        eventBus.register(subscriberOne);
        eventBus.register(subscriberTwo);
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.unregister(subscriberOne);

        // Check that the 2nd subscriber with the same event subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<EventDispatcher> subscribers = eventBus.getDispatchers(eventClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        eventBus.unregister(subscriberTwo);

        assertFalse(eventBus.getDispatchers(eventClass)
                            .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_event_posted() {
        final ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();
        final Event event = Given.AnEvent.projectCreated();
        eventBus.register(subscriber);

        eventBus.post(event);

        // Exclude event ID from comparison.
        assertEquals(event.getMessage(), subscriber.getEventHandled().getMessage());
        assertEquals(event.getContext(), subscriber.getEventHandled().getContext());
    }

    @Test
    public void register_dispatchers() {
        final EventDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        assertTrue(eventBus.getDispatchers(EventClass.of(ProjectCreated.class))
                           .contains(dispatcher));
    }

    @Test
    public void call_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBus.register(dispatcher);

        eventBus.post(Given.AnEvent.projectCreated());

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void not_call_dispatchers_if_dispatcher_event_execution_postponed() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBusWithPosponedExecution.register(dispatcher);

        final Event event = Given.AnEvent.projectCreated();
        eventBusWithPosponedExecution.post(event);
        assertFalse(dispatcher.isDispatchCalled());

        final boolean eventPostponed = postponedDispatcherDelivery.isPostponed(event, dispatcher);
        assertTrue(eventPostponed);
    }

    @Test
    public void deliver_postponed_event_to_dispatcher_using_configured_executor() {
        final BareDispatcher dispatcher = new BareDispatcher();

        eventBusWithPosponedExecution.register(dispatcher);

        final Event event = Given.AnEvent.projectCreated();
        eventBusWithPosponedExecution.post(event);
        final Set<EventEnvelope> postponedEvents = postponedDispatcherDelivery.getPostponedEvents();
        final EventEnvelope postponedEvent = postponedEvents.iterator()
                                                            .next();
        verify(delegateDispatcherExecutor, never()).execute(any(Runnable.class));
        postponedDispatcherDelivery.deliverNow(postponedEvent, dispatcher.getClass());
        assertTrue(dispatcher.isDispatchCalled());
        verify(delegateDispatcherExecutor).execute(any(Runnable.class));
    }

    @Test
    public void unregister_dispatchers() {
        final EventDispatcher dispatcherOne = new BareDispatcher();
        final EventDispatcher dispatcherTwo = new BareDispatcher();
        final EventClass eventClass = EventClass.of(ProjectCreated.class);
        eventBus.register(dispatcherOne);
        eventBus.register(dispatcherTwo);

        eventBus.unregister(dispatcherOne);
        final Set<EventDispatcher> dispatchers = eventBus.getDispatchers(eventClass);

        // Check we don't have 1st dispatcher, but have 2nd.
        assertFalse(dispatchers.contains(dispatcherOne));
        assertTrue(dispatchers.contains(dispatcherTwo));

        eventBus.unregister(dispatcherTwo);
        assertFalse(eventBus.getDispatchers(eventClass)
                            .contains(dispatcherTwo));
    }

    @Test
    public void catch_exceptions_caused_by_subscribers() {
        final FaultySubscriber faultySubscriber = new FaultySubscriber();

        eventBus.register(faultySubscriber);
        eventBus.post(Given.AnEvent.projectCreated());

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Test
    public void assure_that_event_is_valid_and_subscriber_registered() {
        eventBus.register(new ProjectCreatedSubscriber());

        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(),
                                                  responseObserver);
        assertTrue(isValid);
        assertResponseIsOk(responseObserver);
    }

    @Test
    public void assure_that_event_is_valid_and_dispatcher_registered() {
        eventBus.register(new BareDispatcher());

        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(),
                                                  responseObserver);
        assertTrue(isValid);
        assertResponseIsOk(responseObserver);
    }

    @Test
    public void call_onError_if_event_is_invalid() {
        final MessageValidator validator = mock(MessageValidator.class);
        doReturn(newArrayList(ConstraintViolation.getDefaultInstance()))
                .when(validator)
                .validate(any(Message.class));

        final EventBus eventBus = EventBus.newBuilder()
                                          .setStorageFactory(storageFactory)
                                          .setEventValidator(validator)
                                          .build();
        eventBus.register(new ProjectCreatedSubscriber());

        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(),
                                                  responseObserver);

        assertFalse(isValid);
        assertReturnedExceptionAndNoResponse(InvalidEventException.class, responseObserver);
    }

    @Test
    public void call_onError_if_event_is_unsupported() {
        final boolean isValid = eventBus.validate(Given.EventMessage.projectCreated(),
                                                  responseObserver);

        assertFalse(isValid);
        assertReturnedExceptionAndNoResponse(UnsupportedEventException.class, responseObserver);
    }

    @Test
    public void unregister_registries_on_close() throws Exception {
        final EventStore eventStore = spy(mock(EventStore.class));
        final EventBus eventBus = EventBus.newBuilder()
                                          .setEventStore(eventStore)
                                          .build();
        eventBus.register(new BareDispatcher());
        eventBus.register(new ProjectCreatedSubscriber());
        final EventClass eventClass = EventClass.of(ProjectCreated.class);

        eventBus.close();

        assertTrue(eventBus.getDispatchers(eventClass)
                           .isEmpty());
        verify(eventStore).close();
    }

    @Test
    public void have_log() {
        assertNotNull(EventBus.log());
    }

    @Test
    public void do_not_have_Enricher_by_default() {
        assertNull(eventBus.getEnricher());
    }

    @Test
    public void enrich_event_if_it_can_be_enriched() {
        final EventEnricher enricher = mock(EventEnricher.class);
        final Event event = Given.AnEvent.projectCreated();
        doReturn(true).when(enricher)
                      .canBeEnriched(any(Event.class));
        doReturn(event).when(enricher)
                       .enrich(any(Event.class));
        setUp(enricher);
        eventBus.register(new ProjectCreatedSubscriber());

        eventBus.post(event);

        verify(enricher).enrich(any(Event.class));
    }

    @Test
    public void do_not_enrich_event_if_it_cannot_be_enriched() {
        final EventEnricher enricher = mock(EventEnricher.class);
        doReturn(false).when(enricher)
                       .canBeEnriched(any(Event.class));
        setUp(enricher);
        eventBus.register(new ProjectCreatedSubscriber());

        eventBus.post(Given.AnEvent.projectCreated());

        verify(enricher, never()).enrich(any(Event.class));
    }

    @Test
    public void allow_enrichment_configuration_at_runtime_if_enricher_not_set_previously() {
        setUp(null);
        assertNull(eventBus.getEnricher());

        final Class<ProjectId> eventFieldClass = ProjectId.class;
        final Class<String> enrichmentFieldClass = String.class;
        final Function<ProjectId, String> function = new Function<ProjectId, String>() {
            @Override
            public String apply(@Nullable ProjectId input) {
                checkNotNull(input);
                return input.toString();
            }
        };
        eventBus.addFieldEnrichment(eventFieldClass, enrichmentFieldClass, function);
        final EventEnricher enricher = eventBus.getEnricher();
        assertNotNull(enricher);
    }

    @Test
    public void allow_enrichment_configuration_at_runtime_if_enricher_previously_set() {
        final EventEnricher enricher = mock(EventEnricher.class);
        setUp(enricher);

        final Class<ProjectId> eventFieldClass = ProjectId.class;
        final Class<String> enrichmentFieldClass = String.class;
        final Function<ProjectId, String> function = new Function<ProjectId, String>() {
            @Override
            public String apply(@Nullable ProjectId input) {
                checkNotNull(input);
                return input.toString();
            }
        };
        eventBus.addFieldEnrichment(eventFieldClass, enrichmentFieldClass, function);
        verify(enricher).registerFieldEnrichment(eq(eventFieldClass),
                                                 eq(enrichmentFieldClass),
                                                 eq(function));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void not_accept_null_eventFieldClass_passed_as_field_enrichment_configuration_param() {
        eventBus.addFieldEnrichment(null, String.class, mock(Function.class));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void not_accept_null_enrichmentFieldClass_passed_as_field_enrichment_configuration_param() {
        eventBus.addFieldEnrichment(ProjectId.class, null, mock(Function.class));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void not_accept_null_function_passed_as_field_enrichment_configuration_param() {
        eventBus.addFieldEnrichment(ProjectId.class, String.class, null);
    }

    private static void assertResponseIsOk(MemoizingObserver responseObserver) {
        assertEquals(Responses.ok(), responseObserver.firstResponse());
        assertTrue(responseObserver.isCompleted());
        assertNull(responseObserver.getError());
    }

    private static void assertReturnedExceptionAndNoResponse(
            Class<? extends Exception> exceptionClass,
            MemoizingObserver responseObserver) {
        final Throwable error = responseObserver.getError();

        assertNotNull(error);
        final Throwable cause = error.getCause();
        assertEquals(exceptionClass, cause.getClass());
        assertTrue(responseObserver.responses().isEmpty());
    }

    private static class ProjectCreatedSubscriber extends EventSubscriber {

        private Event eventHandled;

        @Subscribe
        public void on(ProjectCreated eventMsg, EventContext context) {
            this.eventHandled = EventTests.createEvent(eventMsg, context);
        }

        private Event getEventHandled() {
            return eventHandled;
        }
    }

    /** The subscriber which throws exception from the subscriber method. */
    private static class FaultySubscriber extends EventSubscriber {

        private boolean methodCalled = false;

        @SuppressWarnings("unused") // It's fine for a faulty subscriber.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            methodCalled = true;
            throw new UnsupportedOperationException(
                    "What did you expect from " +
                    FaultySubscriber.class.getSimpleName() + '?');
        }

        private boolean isMethodCalled() {
            return this.methodCalled;
        }
    }

    /**
     * A simple dispatcher class, which only dispatch and does not have own event
     * subscribing methods.
     */
    private static class BareDispatcher implements EventDispatcher {

        private boolean dispatchCalled = false;

        @Override
        public Set<EventClass> getMessageClasses() {
            return ImmutableSet.of(EventClass.of(ProjectCreated.class));
        }

        @Override
        public void dispatch(EventEnvelope event) {
            dispatchCalled = true;
        }

        private boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    private static class PostponedDispatcherEventDelivery extends DispatcherEventDelivery {

        private final Map<EventEnvelope,
                Class<? extends EventDispatcher>> postponedExecutions = newHashMap();

        private PostponedDispatcherEventDelivery(Executor delegate) {
            super(delegate);
        }

        @Override
        public boolean shouldPostponeDelivery(EventEnvelope event, EventDispatcher consumer) {
            postponedExecutions.put(event, consumer.getClass());
            return true;
        }

        private boolean isPostponed(Event event, EventDispatcher dispatcher) {
            final EventEnvelope envelope = EventEnvelope.of(event);
            final Class<? extends EventDispatcher> actualClass = postponedExecutions.get(envelope);
            final boolean eventPostponed = actualClass != null;
            final boolean dispatcherMatches = eventPostponed && dispatcher.getClass()
                                                                          .equals(actualClass);
            return dispatcherMatches;
        }

        private Set<EventEnvelope> getPostponedEvents() {
            final Set<EventEnvelope> envelopes = postponedExecutions.keySet();
            return envelopes;
        }
    }
}