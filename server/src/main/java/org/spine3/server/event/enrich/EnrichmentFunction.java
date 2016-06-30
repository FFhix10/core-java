/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import org.spine3.server.event.EventBus;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code EnrichmentFunction} defines how a source message class can be transformed into a target message class.
 *
 * <p>{@code EnrichmentFunction}s are used by an {@link EventEnricher} to augment events passed to {@link EventBus}.
 *
 * @param <S> a type of the source object to enrich
 * @param <T> a type of the enrichment
 * @author Alexander Yevsyukov
 */
/* package */ abstract class EnrichmentFunction<S, T> implements Function<S, T> {

    /**
     * We are having the generified class to be able to bound the types of messages and the
     * translation function when building the {@link EventEnricher}.
     *
     * @see EventEnricher.Builder#addFieldEnrichment(Class, Class, Function)
     **/

    private final Class<S> eventClass;

    private final Class<T> enrichmentClass;

    /* package */ EnrichmentFunction(Class<S> eventClass, Class<T> enrichmentClass) {
        this.eventClass = checkNotNull(eventClass);
        this.enrichmentClass = checkNotNull(enrichmentClass);
        checkArgument(!eventClass.equals(enrichmentClass),
                      "Source and target class must not be equal. Passed two values of %", eventClass);
    }

    public Class<S> getEventClass() {
        return eventClass;
    }

    public Class<T> getEnrichmentClass() {
        return enrichmentClass;
    }

    public abstract Function<S, T> getFunction();

    /**
     * Validates the instance.
     *
     * @throws IllegalStateException if the function cannot perform the conversion in its current state
     * or because of the state of its environment
     */
    /* package */ abstract void validate();

    @Override
    public int hashCode() {
        return Objects.hash(eventClass, enrichmentClass);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        //noinspection ConstantConditions
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EnrichmentFunction other = (EnrichmentFunction) obj;
        return Objects.equals(this.eventClass, other.eventClass)
                && Objects.equals(this.enrichmentClass, other.enrichmentClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("eventClass", eventClass)
                          .add("enrichmentClass", enrichmentClass)
                          .toString();
    }
}
