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

package io.spine.testing.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.SerializableMessage;
import io.spine.core.MessageWithContext;
import io.spine.protobuf.AnyPacker;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.extensions.proto.ProtoTruth.protos;

/**
 * Abstract base for subjects checking messages, such as events or commands, generated by
 * a Bounded Context under the test.
 *
 * @param <S>
 *         the self-type for return type covariance
 * @param <T>
 *         the type of the outer objects of the checked messages,
 *         such as {@link io.spine.core.Command Command} or {@link io.spine.core.Event Event}
 * @param <M>
 *         the type of emitted messages, such as {@link io.spine.base.CommandMessage CommandMessage}
 *         or {@link io.spine.base.EventMessage EventMessage}.
 */
public abstract class EmittedMessageSubject<S extends EmittedMessageSubject<S, T, M>,
                                            T extends MessageWithContext,
                                            M extends SerializableMessage>
        extends Subject<S, Iterable<T>> {

    /**
     * Key strings for Truth facts.
     */
    @VisibleForTesting
    enum FactKey {

        MESSAGE_COUNT("the count of the generated messages is"),
        REQUESTED_INDEX("but the requested index was"),
        @SuppressWarnings("DuplicateStringLiteralInspection")
        ACTUAL("actual");

        private final String value;

        FactKey(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    protected EmittedMessageSubject(FailureMetadata metadata, @NullableDecl Iterable<T> actual) {
        super(metadata, actual);
    }

    /** Fails if the subject does not have the given size. */
    public final void hasSize(int expectedSize) {
        assertActual().hasSize(expectedSize);
    }

    /** Fails if the subject is not empty. */
    public final void isEmpty() {
        assertActual().isEmpty();
    }

    /** Fails if the subject is empty. */
    public final void isNotEmpty() {
        assertActual().isNotEmpty();
    }

    private IterableSubject assertActual() {
        return check().that(actual());
    }

    /**
     * Obtains the subject for the message at the given index.
     *
     * <p>Fails if the index is out of the range of the generated message sequence.
     */
    public final ProtoSubject<?, ?> message(int index) {
        int size = Iterables.size(actual());
        if (index >= size) {
            failWithActual(
                    fact(FactKey.MESSAGE_COUNT.value, size),
                    fact(FactKey.REQUESTED_INDEX.value, index)
            );
            return ignoreCheck().about(protos())
                                .that(Empty.getDefaultInstance());
        } else {
            T outerObject = Iterables.get(actual(), index);
            Message unpacked = AnyPacker.unpack(outerObject.getMessage());
            return ProtoTruth.assertThat(unpacked);
        }
    }

    /**
     * Provides factory for creating the same type of subject on a subset of messages.
     */
    protected abstract Subject.Factory<S, Iterable<T>> factory();

    /**
     * Obtains the subject over outer objects that contain messages of the passed class.
     */
    public final S withType(Class<? extends M> messageClass) {
        Iterable<T> actual = actual();
        if (actual == null) {
            failWithActual(fact(FactKey.ACTUAL.value, null));
            return ignoreCheck().about(factory())
                                .that(ImmutableList.of());
        } else {
            List<T> filtered =
                    Streams.stream(actual)
                           .filter(m -> m.is(messageClass))
                           .collect(toImmutableList());
            return check().about(factory())
                          .that(filtered);
        }
    }
}
