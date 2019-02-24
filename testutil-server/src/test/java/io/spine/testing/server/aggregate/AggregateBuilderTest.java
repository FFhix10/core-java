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

package io.spine.testing.server.aggregate;

import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.server.aggregate.Aggregate;
import io.spine.testing.server.given.entity.TuProject;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.TuProjectVBuilder;
import io.spine.time.testing.TimeTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AggregateBuilder should create an aggregate with requested...")
class AggregateBuilderTest {

    private TuProjectId id;
    private int version;
    private TuProject state;
    private Timestamp whenModified;

    private Aggregate aggregate;

    @BeforeEach
    void setUp() {
        id = TuProjectId.newBuilder()
                        .setValue(Identifier.newUuid())
                        .build();
        version = 2019;
        whenModified = Time.getCurrentTime();
        state = TuProject.newBuilder()
                         .setId(id)
                         .setTimestamp(TimeTests.Past.minutesAgo(60))
                         .build();

        aggregate = givenAggregate()
                .withId(id)
                .withVersion(version)
                .withState(state)
                .modifiedOn(whenModified)
                .build();
    }

    @Test
    @DisplayName("class")
    void requestedClass() {
        assertEquals(TestAggregate.class, aggregate.getClass());
    }

    @Test
    @DisplayName("ID")
    void id() {
        assertEquals(id, aggregate.id());
    }

    @Test
    @DisplayName("state")
    void state() {
        assertEquals(state, aggregate.state());
    }

    @Test
    @DisplayName("version")
    void version() {
        assertEquals(version, aggregate.getVersion().getNumber());
    }

    @Test
    @DisplayName("modification time")
    void timestamp() {
        assertEquals(whenModified, aggregate.whenModified());
    }

    /*
     * Test Environment
     ************************/

    private static AggregateBuilder<TestAggregate, TuProjectId, TuProject> givenAggregate() {
        AggregateBuilder<TestAggregate, TuProjectId, TuProject> result = new AggregateBuilder<>();
        result.setResultClass(TestAggregate.class);
        return result;
    }

    private static class TestAggregate
            extends Aggregate<TuProjectId, TuProject, TuProjectVBuilder> {
        protected TestAggregate(TuProjectId id) {
            super(id);
        }
    }
}
