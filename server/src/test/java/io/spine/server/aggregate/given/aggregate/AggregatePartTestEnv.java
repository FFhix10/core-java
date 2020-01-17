/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.given.aggregate;

import io.spine.core.UserId;
import io.spine.test.aggregate.command.AggAddComment;
import io.spine.test.aggregate.command.AggCreateTask;
import io.spine.test.aggregate.task.AggTaskId;

import static io.spine.base.Identifier.newUuid;

public final class AggregatePartTestEnv {

    public static final AggTaskId ID = AggTaskId
            .newBuilder()
            .setId(newUuid())
            .build();

    public static final UserId ASSIGNEE = UserId
            .newBuilder()
            .setValue(newUuid())
            .build();

    public static final UserId COMMENT_AUTHOR = UserId
            .newBuilder()
            .setValue(newUuid())
            .build();

    /**
     * Prevents the utility class instantiation.
     */
    private AggregatePartTestEnv() {
    }

    public static AggCreateTask createTask() {
        AggCreateTask command = AggCreateTask
                .newBuilder()
                .setTaskId(ID)
                .setAssignee(ASSIGNEE)
                .build();
        return command;
    }

    public static AggAddComment commentTask() {
        AggAddComment command = AggAddComment
                .newBuilder()
                .setTaskId(ID)
                .setAuthor(COMMENT_AUTHOR)
                .setText("ASAP")
                .build();
        return command;
    }
}
