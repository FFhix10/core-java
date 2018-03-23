/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.rejection;

import com.google.protobuf.Empty;
import io.spine.core.CommandClass;
import io.spine.core.RejectionClass;
import io.spine.server.model.HandlerKey;

/**
 * A key for a rejection handler method.
 *
 * <p>The key always contains {@link RejectionClass}, but {@link CommandClass} is optional
 * because a rejection handler doesn't necessarily has a command message as a parameter.
 *
 * @author Dmytro Grankin
 */
class RejectionHandlerKey extends HandlerKey<RejectionClass> {

    private RejectionHandlerKey(RejectionClass rejectionClass,
                                CommandClass commandClass) {
        super(rejectionClass, commandClass);
    }

    static RejectionHandlerKey of(RejectionClass rejectionClass, CommandClass commandClass) {
        return new RejectionHandlerKey(rejectionClass, commandClass);
    }

    static RejectionHandlerKey of(RejectionClass rejectionClass) {
        return new RejectionHandlerKey(rejectionClass, CommandClass.of(Empty.class));
    }
}
