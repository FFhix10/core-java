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

import io.spine.annotation.SPI;
import io.spine.core.ActorContext;
import io.spine.core.MessageId;
import io.spine.core.Signal;
import io.spine.core.SignalId;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@SPI
public abstract class AbstractTracer implements Tracer {

    private final Signal<?, ?, ?> signal;

    protected AbstractTracer(Signal<?, ?, ?> signal) {
        this.signal = checkNotNull(signal);
    }

    @Override
    public SignalId root() {
        return signal.rootMessage()
                     .asSignalId();
    }

    @Override
    public Optional<SignalId> parent() {
        return signal.parent()
                     .map(MessageId::asSignalId);
    }

    @Override
    public ActorContext actor() {
        return signal.actorContext();
    }
}
