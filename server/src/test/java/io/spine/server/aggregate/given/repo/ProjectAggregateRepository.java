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

package io.spine.server.aggregate.given.repo;

import com.google.common.collect.ImmutableSet;
import io.spine.core.EventContext;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.route.EventRoute;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectDeleted;

import java.util.Optional;
import java.util.Set;

/**
 * The repository of positive scenarios
 * {@linkplain io.spine.server.aggregate.given.repo.ProjectAggregate aggregates}.
 *
 * <p>It also widens visibility of the
 * {@link io.spine.server.aggregate.AggregateRepository#store(io.spine.server.aggregate.Aggregate)}
 * and
 * {@link io.spine.server.aggregate.AggregateRepository#aggregateStorage()} methods so they can be
 * used in this test env.
 */
public class ProjectAggregateRepository
        extends AggregateRepository<ProjectId, ProjectAggregate> {

    public static final ProjectId troublesome = ProjectId.newBuilder()
                                                         .setId("INVALID_ID")
                                                         .build();

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public ProjectAggregateRepository() {
        super();
        eventRouting()
                .route(AggProjectArchived.class,
                       new EventRoute<ProjectId, AggProjectArchived>() {
                           private static final long serialVersionUID = 0L;

                           @Override
                           public Set<ProjectId> apply(AggProjectArchived msg, EventContext ctx) {
                               return ImmutableSet.copyOf(msg.getChildProjectIdList());
                           }
                       })
                .route(AggProjectDeleted.class,
                       new EventRoute<ProjectId, AggProjectDeleted>() {
                           private static final long serialVersionUID = 0L;

                           @Override
                           public Set<ProjectId> apply(AggProjectDeleted msg, EventContext ctx) {
                               return ImmutableSet.copyOf(msg.getChildProjectIdList());
                           }
                       });
    }

    @Override
    public Optional<ProjectAggregate> find(ProjectId id) {
        if (id.equals(troublesome)) {
            return Optional.empty();
        }
        return super.find(id);
    }

    @Override
    public AggregateStorage<ProjectId> aggregateStorage() {
        return super.aggregateStorage();
    }

    void storeAggregate(ProjectAggregate aggregate) {
        store(aggregate);
    }
}
