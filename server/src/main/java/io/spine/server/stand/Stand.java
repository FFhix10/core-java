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
package io.spine.server.stand;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.core.MessageId;
import io.spine.core.Origin;
import io.spine.core.Response;
import io.spine.core.Responses;
import io.spine.core.TenantId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.Identity;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityRecordChange;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.server.entity.model.StateClass;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.tenant.QueryOperation;
import io.spine.server.tenant.SubscriptionOperation;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.SystemReadSide;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.union;
import static io.spine.client.Queries.typeOf;
import static io.spine.grpc.StreamObservers.ack;
import static java.util.Collections.singleton;

/**
 * A container for storing the latest {@link io.spine.server.aggregate.Aggregate Aggregate}
 * states.
 *
 * <p>Provides an optimal way to access the latest state of published aggregates
 * for read-side services. The aggregate states are delivered to the instance of {@code Stand}
 * from {@link AggregateRepository} instances.
 *
 * <p>In order to provide a flexibility in defining data access policies,
 * {@code Stand} contains only the states of published aggregates.
 * Please refer to {@link io.spine.server.aggregate.Aggregate Aggregate} for details on
 * publishing aggregates.
 *
 * <p>Each {@link io.spine.server.BoundedContext BoundedContext} contains only one
 * instance of {@code Stand}.
 */
@SuppressWarnings("OverlyCoupledClass")
public class Stand extends AbstractEventSubscriber implements AutoCloseable {

    /**
     * Used to return an empty result collection for {@link Query}.
     */
    private static final QueryProcessor NO_OP_PROCESSOR = new NoOpQueryProcessor();

    /**
     * Manages the subscriptions for this instance of {@code Stand}.
     */
    private final SubscriptionRegistry subscriptionRegistry;

    /**
     * Manages the entity {@linkplain TypeUrl types} exposed via this instance of {@code Stand}.
     */
    private final TypeRegistry typeRegistry;

    /**
     * Manages the events produced by the associated repositories.
     */
    private final EventRegistry eventRegistry;

    private final boolean multitenant;

    private final TopicValidator topicValidator;
    private final QueryValidator queryValidator;
    private final SubscriptionValidator subscriptionValidator;

    private final AggregateQueryProcessor aggregateQueryProcessor;

    private Stand(Builder builder) {
        super();
        this.multitenant = builder.multitenant != null
                           ? builder.multitenant
                           : false;
        this.subscriptionRegistry = builder.subscriptionRegistry();
        this.typeRegistry = builder.typeRegistry();
        this.eventRegistry = builder.eventRegistry();
        this.topicValidator = builder.topicValidator();
        this.queryValidator = builder.queryValidator();
        this.subscriptionValidator = builder.subscriptionValidator();
        this.aggregateQueryProcessor = new AggregateQueryProcessor(builder.systemReadSide());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Test-only method that posts the state of an entity to this stand.
     *
     * @param entity
     *         the entity whose state to post
     * @param lifecycle
     *         the lifecycle of the entity
     * @implNote The only purpose of this method is to deliver the new entity state to the
     *         subscribers through the artificially created {@link EntityStateChanged} event.
     *         It doesn't do any proper lifecycle management ignoring "archived"/"deleted" actions,
     *         IDs of applied messages, etc.
     */
    @VisibleForTesting
    void post(Entity entity, EntityLifecycle lifecycle) {
        Any id = Identifier.pack(entity.id());
        Any state = AnyPacker.pack(entity.state());
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(id)
                .setState(state)
                .vBuild();
        EntityRecordChange change = EntityRecordChange
                .newBuilder()
                .setNewValue(record)
                .vBuild();
        MessageId origin = Identity.byString("Stand-received-entity-update");
        lifecycle.onStateChanged(change,
                                 ImmutableSet.of(origin),
                                 Origin.getDefaultInstance());
    }

    /**
     * Receives an event and notifies matching subscriptions.
     */
    @Override
    protected void handle(EventEnvelope event) {
        TypeUrl typeUrl = event.typeUrl();
        if (subscriptionRegistry.hasType(typeUrl)) {
            subscriptionRegistry.byType(typeUrl)
                                .stream()
                                .filter(SubscriptionRecord::isActive)
                                .forEach(record -> record.update(event));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code true} as the filtering happens in {@link #handle(EventEnvelope)}.
     */
    @Override
    public boolean canDispatch(EventEnvelope event) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>As dynamically changed event classes for subscribers are currently not supported,
     * {@code Stand} receives all events produced by the associated repositories and then notifies
     * subscriptions if necessary.
     *
     * <p>Also receives {@link EntityStateChanged} event class to enable entity subscriptions.
     */
    @Override
    public Set<EventClass> messageClasses() {
        Set<EventClass> result =
                union(eventRegistry.eventClasses(), singleton(StateClass.updateEvent()));
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stand does not consume external events.
     */
    @Override
    public Set<EventClass> externalEventClasses() {
        return ImmutableSet.of();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stand only consumes the domestic events.
     */
    @Override
    public Set<EventClass> domesticEventClasses() {
        return eventClasses();
    }

    @Internal
    @VisibleForTesting
    public boolean isMultitenant() {
        return multitenant;
    }

    /**
     * Subscribes to the updates of entity state or to the specific types of events, according to
     * {@link Topic}.
     *
     * <p>Once this instance of {@code Stand} receives an update of an entity or a matching event
     * with the given {@code TypeUrl}, all such callbacks are executed.
     *
     * @param topic
     *         a {@link Topic} defining the subscription target
     */
    public void subscribe(Topic topic, StreamObserver<Subscription> responseObserver)
            throws InvalidRequestException {
        topicValidator.validate(topic);

        TenantId tenantId = topic.getContext()
                                 .getTenantId();
        TenantAwareOperation op = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                Subscription subscription = subscriptionRegistry.add(topic);
                responseObserver.onNext(subscription);
                responseObserver.onCompleted();
            }
        };
        op.execute();
    }

    /**
     * Activates the subscription created via {@link #subscribe(Topic, StreamObserver)
     * subscribe() method call}.
     *
     * <p>After the activation, the clients will start receiving the updates via
     * {@code SubscriptionCallback} upon entity state changes or new events arrival.
     *
     * @param subscription
     *         the subscription to activate
     * @param callback
     *         the action which notifies the subscribers about an update
     * @param responseObserver
     *         an observer to notify of a successful acknowledgement of the subscription activation.
     * @see #subscribe(Topic, StreamObserver)
     */
    public void activate(Subscription subscription, SubscriptionCallback callback,
                         StreamObserver<Response> responseObserver)
            throws InvalidRequestException {
        checkNotNull(subscription);
        checkNotNull(callback);

        subscriptionValidator.validate(subscription);

        SubscriptionOperation op = new SubscriptionOperation(subscription) {
            @Override
            public void run() {
                subscriptionRegistry.activate(subscription, callback);
                ack(responseObserver);
            }
        };

        op.execute();
    }

    /**
     * Cancels the {@link Subscription}.
     *
     * <p>Typically invoked to cancel the previous
     * {@link #activate(Subscription, SubscriptionCallback, StreamObserver) activate()} call.
     *
     * <p>After this method is called, the subscribers stop receiving the updates,
     * related to the given {@code Subscription}.
     *
     * @param subscription
     *         the subscription to cancel
     * @param responseObserver
     *         an observer to notify of a successful acknowledgement of the subscription
     *         cancellation.
     */
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver)
            throws InvalidRequestException {
        subscriptionValidator.validate(subscription);

        SubscriptionOperation op = new SubscriptionOperation(subscription) {
            @Override
            public void run() {
                subscriptionRegistry.remove(subscription);
                ack(responseObserver);
            }
        };
        op.execute();
    }

    /**
     * Reads all {@link Entity} types exposed for reading by this instance of {@code Stand}.
     *
     * <p>In order to expose the type, use {@link Stand#registerTypeSupplier(Repository)}.
     *
     * <p>The result includes all values from {@link #exposedAggregateTypes()} as well.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    public ImmutableSet<TypeUrl> exposedTypes() {
        return typeRegistry.allTypes();
    }

    /**
     * Reads all event types produced by the repositories associated with this {@code Stand}.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    public ImmutableSet<TypeUrl> exposedEventTypes() {
        return eventRegistry.typeSet();
    }

    /**
     * Reads all {@link io.spine.server.aggregate.Aggregate Aggregate} entity types
     * exposed for reading by this instance of {@code Stand}.
     *
     * <p>Use {@link Stand#registerTypeSupplier(Repository)} to expose an {@code Aggregate} type.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    public ImmutableSet<TypeUrl> exposedAggregateTypes() {
        return typeRegistry.aggregateTypes();
    }

    /**
     * Reads a particular set of items from the read-side of the application and
     * feed the result into an instance.
     *
     * <p>{@link Query} defines the query target and the expected detail level for response.
     *
     * <p>The query results are fed to an instance
     * of {@link StreamObserver}&lt;{@link QueryResponse}&gt;.
     *
     * @param query
     *         the instance of query
     * @param responseObserver
     *         the observer to feed the query results to
     */
    public void execute(Query query, StreamObserver<QueryResponse> responseObserver)
            throws InvalidRequestException {
        queryValidator.validate(query);

        TypeUrl type = typeOf(query);
        QueryProcessor queryProcessor = processorFor(type);

        QueryOperation op = new QueryOperation(query) {
            @Override
            public void run() {
                Collection<EntityStateWithVersion> readResult = queryProcessor.process(query());
                QueryResponse response = QueryResponse
                        .newBuilder()
                        .addAllMessage(readResult)
                        .setResponse(Responses.ok())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        op.execute();
    }

    /**
     * Registers a {@code Repository} as an entity/event type supplier.
     *
     * <p>In case of an {@link AggregateRepository}, the repository is not registered as
     * a supplier for read operations, since the {@code Aggregate} reads are performed by
     * accessing the latest state in the corresponding {@code MirrorProjection}.
     *
     * <p>However, the type of the {@code AggregateRepository} instance is recorded for
     * the postponed processing of updates.
     */
    public <I, E extends Entity<I, ?>> void registerTypeSupplier(Repository<I, E> repository) {
        typeRegistry.register(repository);
        eventRegistry.register(repository);
    }

    /**
     * Dumps all {@link TypeUrl}-to-{@link RecordBasedRepository} relations.
     */
    @Override
    public void close() throws Exception {
        typeRegistry.close();
        eventRegistry.close();
    }

    /**
     * Delivers the given subscription update to the read-side.
     *
     * @see #activate(Subscription, SubscriptionCallback, StreamObserver)
     * @see #cancel(Subscription, StreamObserver)
     */
    public interface SubscriptionCallback extends Consumer<SubscriptionUpdate> {

    }

    /**
     * Factory method which determines a proper {@link QueryProcessor} implementation
     * depending on {@link TypeUrl} of the incoming {@link Query#getTarget()}.
     *
     * <p>As {@code Stand} accumulates the read-side updates from various repositories,
     * the {@code Query} processing varies a lot. The target type of the incoming {@code Query}
     * tells the {@code Stand} about the essence of the object queried. Thus making it possible
     * to pick a proper strategy for data fetch.
     *
     * @param type
     *         the target type of the {@code Query}
     * @return suitable implementation of {@code QueryProcessor}
     */
    private QueryProcessor processorFor(TypeUrl type) {
        Optional<? extends RecordBasedRepository<?, ?, ?>> foundRepository =
                typeRegistry.recordRepositoryOf(type);
        if (foundRepository.isPresent()) {
            RecordBasedRepository<?, ?, ?> repository = foundRepository.get();
            return new EntityQueryProcessor(repository);
        } else if (exposedAggregateTypes().contains(type)) {
            return aggregateProcessor();
        } else {
            return NO_OP_PROCESSOR;
        }
    }

    private QueryProcessor aggregateProcessor() {
        return aggregateQueryProcessor;
    }

    @CanIgnoreReturnValue
    public static class Builder {

        /**
         * The multi-tenancy flag for the {@code Stand} to build.
         *
         * <p>The value of this field should be equal to that of corresponding
         * {@linkplain io.spine.server.BoundedContextBuilder BoundedContextBuilder} and is not
         * supposed to be {@linkplain #setMultitenant(Boolean) set directly}.
         *
         * <p>If set directly, the value would be matched to the multi-tenancy flag of aggregating
         * {@code BoundedContext}.
         */
        private @Nullable Boolean multitenant;

        private final TypeRegistry typeRegistry = InMemoryTypeRegistry.newInstance();
        private final EventRegistry eventRegistry = InMemoryEventRegistry.newInstance();

        private SubscriptionRegistry subscriptionRegistry;
        private TopicValidator topicValidator;
        private QueryValidator queryValidator;
        private SubscriptionValidator subscriptionValidator;
        private SystemReadSide systemReadSide;

        @Internal
        public Builder setMultitenant(@Nullable Boolean multitenant) {
            this.multitenant = multitenant;
            return this;
        }

        @Internal
        public Builder setSystemReadSide(SystemReadSide readSide) {
            this.systemReadSide = checkNotNull(readSide);
            return this;
        }

        @Internal
        public @Nullable Boolean isMultitenant() {
            return multitenant;
        }

        private SubscriptionRegistry subscriptionRegistry() {
            return subscriptionRegistry;
        }

        private TopicValidator topicValidator() {
            return topicValidator;
        }

        private QueryValidator queryValidator() {
            return queryValidator;
        }

        private SubscriptionValidator subscriptionValidator() {
            return subscriptionValidator;
        }

        private TypeRegistry typeRegistry() {
            return typeRegistry;
        }

        private EventRegistry eventRegistry() {
            return eventRegistry;
        }

        private SystemReadSide systemReadSide() {
            return systemReadSide;
        }

        /**
         * Builds an instance of {@code Stand}.
         *
         * <p>This method is supposed to be called internally when building aggregating
         * {@code BoundedContext}.
         *
         * @return new instance of Stand
         */
        @CheckReturnValue
        @Internal
        public Stand build() {
            checkState(systemReadSide != null, "SystemWriteSide is not set.");
            boolean multitenant = this.multitenant == null
                                  ? false
                                  : this.multitenant;
            subscriptionRegistry = MultitenantSubscriptionRegistry.newInstance(multitenant);
            topicValidator = new TopicValidator(typeRegistry, eventRegistry);
            queryValidator = new QueryValidator(typeRegistry);
            subscriptionValidator = new SubscriptionValidator(subscriptionRegistry);

            Stand result = new Stand(this);
            return result;
        }
    }
}
