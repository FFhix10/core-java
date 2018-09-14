/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Maps.transformValues;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isRecordWithColumnsVisible;
import static io.spine.server.entity.FieldMasks.applyMask;
import static io.spine.server.storage.memory.EntityRecordComparator.inOrder;
import static java.util.stream.Collectors.toMap;

/**
 * The memory-based storage for {@link EntityRecord} that represents
 * all storage operations available for data of a single tenant.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
class TenantRecords<I> implements TenantStorage<I, EntityRecordWithColumns> {

    private final Map<I, EntityRecordWithColumns> records = newConcurrentMap();
    private final Map<I, EntityRecordWithColumns> filtered =
            filterValues(records, isRecordWithColumnsVisible()::test);
    private static final EntityRecordUnpacker UNPACKER = EntityRecordUnpacker.INSTANCE;

    @Override
    public Iterator<I> index() {
        Iterator<I> result = filtered.keySet()
                                     .iterator();
        return result;
    }

    @Override
    public void put(I id, EntityRecordWithColumns record) {
        records.put(id, record);
    }

    @Override
    public Optional<EntityRecordWithColumns> get(I id) {
        EntityRecordWithColumns record = records.get(id);
        return Optional.ofNullable(record);
    }

    boolean delete(I id) {
        return records.remove(id) != null;
    }

    private Map<I, EntityRecordWithColumns> filtered() {
        return filtered;
    }

    Map<I, EntityRecord> readAllRecords() {
        Map<I, EntityRecordWithColumns> filtered = filtered();
        Map<I, EntityRecord> records = unpackRecords(filtered);
        ImmutableMap<I, EntityRecord> result = ImmutableMap.copyOf(records);
        return result;
    }

    Map<I, EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        Map<I, EntityRecordWithColumns> internalRecords = findRecords(query);
        Map<I, EntityRecord> records = unpackRecords(internalRecords);
        Map<I, EntityRecord> maskedRecords = maskRecords(records, fieldMask);
        ImmutableMap<I, EntityRecord> result = ImmutableMap.copyOf(maskedRecords);
        return result;
    }

    /**
     * Applies a field mask to the provided records.
     */
    private Map<I, EntityRecord> maskRecords(Map<I, EntityRecord> records, FieldMask fieldMask) {
        Function<EntityRecord, EntityRecord> fieldMaskApplier = new FieldMaskApplier(fieldMask);
        return transformValues(records, fieldMaskApplier::apply);
    }

    /**
     * Gets the {@link EntityRecord} values from {@link EntityRecordWithColumns} entries.
     */
    private Map<I, EntityRecord> unpackRecords(Map<I, EntityRecordWithColumns> records) {
        return transformValues(records, UNPACKER::apply);
    }

    private Map<I, EntityRecordWithColumns> findRecords(EntityQuery<I> query) {
        Map<I, EntityRecordWithColumns> filtered = filterRecords(query);
        QueryParameters parameters = query.getParameters();
        Stream<Map.Entry<I, EntityRecordWithColumns>> stream = filtered.entrySet()
                                                                       .stream();
        if (parameters.ordered()) {
            stream = stream.sorted(inOrder(parameters.order()));
        }
        if (parameters.limited()) {
            stream = stream.limit(parameters.limit());
        }
        return stream.collect(entriesToMap());
    }

    /**
     * Filters the records returning only the ones matching the
     * {@linkplain EntityQuery entity query}.
     */
    private Map<I, EntityRecordWithColumns> filterRecords(EntityQuery<I> query) {
        EntityQueryMatcher<I> matcher = new EntityQueryMatcher<>(query);
        return filterValues(records, matcher::test);
    }

    /**
     * A generic collector of {@link Map.Entry} instances back to a {@link Map}.
     */
    private static <A, B> Collector<Map.Entry<A, B>, ?, Map<A, B>>
    entriesToMap() {
        return toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    @Nullable
    EntityRecord findAndApplyFieldMask(I givenId, FieldMask fieldMask) {
        EntityRecord result = null;
        for (I recordId : filtered.keySet()) {
            if (recordId.equals(givenId)) {
                Optional<EntityRecordWithColumns> record = get(recordId);
                if (!record.isPresent()) {
                    continue;
                }
                EntityRecord.Builder matchingRecord = record.get()
                                                            .getRecord()
                                                            .toBuilder();
                Any state = matchingRecord.getState();
                TypeUrl typeUrl = TypeUrl.parse(state.getTypeUrl());
                Message wholeState = unpack(state);
                Message maskedState = applyMask(fieldMask, wholeState, typeUrl);
                Any processed = pack(maskedState);

                matchingRecord.setState(processed);
                result = matchingRecord.build();
            }
        }
        return result;
    }

    Map<I, EntityRecord> readAllRecords(FieldMask fieldMask) {
        if (fieldMask.getPathsList()
                     .isEmpty()) {
            return readAllRecords();
        }

        if (isEmpty()) {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<I, EntityRecord> result = ImmutableMap.builder();

        for (Map.Entry<I, EntityRecordWithColumns> storageEntry : filtered.entrySet()) {
            I id = storageEntry.getKey();
            EntityRecord rawRecord = storageEntry.getValue()
                                                 .getRecord();
            TypeUrl type = TypeUrl.parse(rawRecord.getState()
                                                  .getTypeUrl());
            Any recordState = rawRecord.getState();
            Message stateAsMessage = unpack(recordState);
            Message processedState = applyMask(fieldMask, stateAsMessage, type);
            Any packedState = pack(processedState);
            EntityRecord resultingRecord = EntityRecord
                    .newBuilder()
                    .setState(packedState)
                    .build();
            result.put(id, resultingRecord);
        }

        return result.build();
    }

    @Override
    public boolean isEmpty() {
        return filtered.isEmpty();
    }

    /**
     * A {@link Function} transforming the {@link EntityRecord} state by applying the given
     * {@link FieldMask} to it.
     *
     * <p>The resulting {@link EntityRecord} has the same fields as the given one except
     * the {@code state} field, which is masked.
     */
    private static class FieldMaskApplier implements Function<EntityRecord, EntityRecord> {

        private final FieldMask fieldMask;

        private FieldMaskApplier(FieldMask fieldMask) {
            this.fieldMask = fieldMask;
        }

        @Override
        public @Nullable EntityRecord apply(@Nullable EntityRecord input) {
            checkNotNull(input);
            Any packedState = input.getState();
            Message state = unpack(packedState);
            TypeUrl typeUrl = TypeUrl.ofEnclosed(packedState);
            Message maskedState = applyMask(fieldMask, state, typeUrl);
            Any repackedState = pack(maskedState);
            EntityRecord result = EntityRecord
                    .newBuilder(input)
                    .setState(repackedState)
                    .build();
            return result;
        }
    }
}
