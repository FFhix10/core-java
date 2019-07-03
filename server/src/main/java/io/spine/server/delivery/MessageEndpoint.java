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

package io.spine.server.delivery;

import io.spine.annotation.Internal;
import io.spine.server.entity.Repository;
import io.spine.server.type.ActorMessageEnvelope;

/**
 * An endpoint for messages delivered to an abstract target.
 *
 * @param <I>
 *         the type of target identifier
 * @param <M>
 *         the type of message envelope being delivered
 */
@Internal
public interface MessageEndpoint<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    /**
     * Dispatches the message to the target with the passed ID.
     *
     * @param targetId
     *         the identifier of a target
     */
    void dispatchTo(I targetId);

    /**
     * Processes the exception thrown during dispatching the message.
     */
    void onError(M envelope, RuntimeException exception);

    void onDuplicate(I target, M envelope);

    Repository<I, ?> repository();
}
