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

package io.spine.server.storage.given;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.TargetFilters;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.HasLifecycleColumns;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.TestTransaction;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordStorage;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.ProjectWithColumns;
import io.spine.testing.core.given.GivenVersion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.TestTransaction.injectState;
import static io.spine.server.entity.storage.TestEntityRecordWithColumnsFactory.createRecord;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecordStorageTestEnv {

    /** Prevents instantiation of this utility class. */
    private RecordStorageTestEnv() {
    }

    public static EntityRecord buildStorageRecord(ProjectId id, Message state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    public static EntityRecord buildStorageRecord(ProjectId id, Message state,
                                                  LifecycleFlags lifecycleFlags) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .setLifecycleFlags(lifecycleFlags)
                .build();
        return record;
    }

    /**
     * Creates new instance of the test entity.
     */
    public static TestCounterEntity newEntity(ProjectId id) {
        TestCounterEntity entity = new TestCounterEntity(id);
        injectState(entity, Project.newBuilder().setId(id).build(), Versions.zero());
        return entity;
    }

    public static void archive(TransactionalEntity<ProjectId, ?, ?> entity) {
        TestTransaction.archive(entity);
    }

    public static void delete(TransactionalEntity<ProjectId, ?, ?> entity) {
        TestTransaction.delete(entity);
    }

    public static EntityRecordWithColumns withLifecycleColumns(EntityRecord record) {
        LifecycleFlags flags = record.getLifecycleFlags();
        Map<ColumnName, Object> columns = ImmutableMap.of(
                ColumnName.of(archived),
                flags.getArchived(),
                ColumnName.of(deleted),
                flags.getDeleted()
        );
        EntityRecordWithColumns result = createRecord(record, columns);
        return result;
    }

    public static void assertSingleRecord(EntityRecord expected, Iterator<EntityRecord> actual) {
        assertTrue(actual.hasNext());
        EntityRecord singleRecord = actual.next();
        assertFalse(actual.hasNext());
        assertEquals(expected, singleRecord);
    }

    public static <T> EntityQuery<T>
    newEntityQuery(TargetFilters filters, RecordStorage<T> storage) {
        return EntityQueries.from(filters, storage);
    }

    public static TargetFilters emptyFilters() {
        return TargetFilters.getDefaultInstance();
    }

    public static <E> void assertIteratorsEqual(Iterator<? extends E> first,
                                                Iterator<? extends E> second) {
        Collection<? extends E> firstCollection = newArrayList(first);
        Collection<? extends E> secondCollection = newArrayList(second);
        assertEquals(firstCollection.size(), secondCollection.size());
        assertThat(firstCollection).containsExactlyElementsIn(secondCollection);
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestCounterEntity
            extends TransactionalEntity<ProjectId, Project, Project.Builder>
            implements ProjectWithColumns, HasLifecycleColumns<ProjectId, Project> {

        private int counter = 0;

        private TestCounterEntity(ProjectId id) {
            super(id);
        }

        @Override
        public String getIdString() {
            return idAsString();
        }

        @Override
        public boolean getDoable() {
            return false;
        }

        @Override
        public Any getWrappedState() {
            return Any.getDefaultInstance();
        }

        @Override
        public int getProjectStatusValue() {
            return state().getStatusValue();
        }

        @Override
        public Version getCounterVersion() {
            return Version.newBuilder()
                          .setNumber(counter)
                          .build();
        }

        public void assignStatus(Project.Status status) {
            Project newState = Project
                    .newBuilder(state())
                    .setStatus(status)
                    .build();
            injectState(this, newState, getCounterVersion());
        }

        public void assignCounter(int counter) {
            this.counter = counter;
        }
    }
}
