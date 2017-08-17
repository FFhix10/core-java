/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.procman;

import io.spine.annotation.SPI;
import io.spine.core.CommandEnvelope;

/**
 * @author Alex Tymchenko
 */
@SPI
public abstract class PmCommandDelivery<I, P extends ProcessManager<I, ?, ?>>
        extends PmEndpointDelivery<I, P, CommandEnvelope> {

    protected PmCommandDelivery(ProcessManagerRepository<I, P, ?> repository) {
        super(repository);
    }

    @Override
    protected PmCommandEndpoint<I, P> getEndpoint(CommandEnvelope envelope) {
        return PmCommandEndpoint.of(repository(), envelope);
    }

    public static <I, A extends ProcessManager<I, ?, ?>>
    PmCommandDelivery<I, A> directDelivery(ProcessManagerRepository<I, A, ?> repository) {
        return new Direct<>(repository);
    }

    /**
     * Direct delivery which does not postpone dispatching.
     *
     * @param <I> the type of process manager IDs
     * @param <P> the type of process manager
     */
    public static class Direct<I, P extends ProcessManager<I, ?, ?>>
            extends PmCommandDelivery<I, P> {

        private Direct(ProcessManagerRepository<I, P, ?> repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(I id, CommandEnvelope envelope) {
            return false;
        }
    }
}

