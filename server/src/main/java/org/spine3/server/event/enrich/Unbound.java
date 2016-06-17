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
import com.google.protobuf.Message;
import org.spine3.util.Exceptions;

import javax.annotation.Nullable;

/**
 * An interim entry in the {@link EventEnricher.Builder} to hold information about the types.
 *
 * <p>Instances of this class are converted to real version via {@link #toBound(EventEnricher)} method.
 */
/* package */ class Unbound<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

    private static final String CANNOT_USE_MSG = "Unbound instance cannot be used for enrichments";

    /* package */Unbound(Class<M> sourceClass, Class<E> targetClass) {
        super(sourceClass, targetClass);
    }

    /**
     * @return always {@code null}
     */
    @Nullable
    @Override
    public Function<M, E> getFunction() {
        return null;
    }

    private static <M extends Message, E extends Message> Function<M, E> empty() {
        return new Function<M, E>() {
            @Nullable
            @Override
            public E apply(@Nullable M input) {
                return null;
            }
        };
    }

    /**
     * Converts the instance into the {@link EventMessageEnricher}.
     */
    /* package */ EventMessageEnricher<M, E> toBound(EventEnricher enricher) {
        return new EventMessageEnricher<>(enricher,
                                          getSourceClass(),
                                          getTargetClass());
    }

    /**
     * @throws IllegalStateException always to prevent the usage of instances in configured {@link EventEnricher}
     */
    @Override
    /* package */void validate() {
        throw Exceptions.unsupported(CANNOT_USE_MSG);
    }

    @Nullable
    @Override
    public E apply(@Nullable M m) {
        throw Exceptions.unsupported(CANNOT_USE_MSG);
    }
}
