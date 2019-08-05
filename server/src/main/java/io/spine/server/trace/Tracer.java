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

package io.spine.server.trace;

import io.spine.core.MessageId;
import io.spine.core.Signal;
import io.spine.system.server.EntityTypeName;

/**
 * A tracer of a single signal.
 *
 * <p>Implementations may choose to generate traces on {@link #processedBy} or on {@code close()}
 * for bulk processing.
 */
public interface Tracer extends AutoCloseable {

    /**
     * Obtains the traced signal message.
     */
    Signal<?, ?, ?> signal();

    /**
     * Marks the message to be processed by an entity with the given {@link MessageId}.
     *
     * <p>This method is invoked after the message is processed.
     *
     * @param receiver
     *         the entity handling the signal
     * @param receiverType
     *         the class of the entity handling the signal
     */
    void processedBy(MessageId receiver, EntityTypeName receiverType);
}