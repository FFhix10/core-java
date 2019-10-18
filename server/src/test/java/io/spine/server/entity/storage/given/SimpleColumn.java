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

package io.spine.server.entity.storage.given;

import com.google.protobuf.Timestamp;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.Columns;
import io.spine.server.projection.Projection;
import io.spine.test.entity.TaskView;
import io.spine.test.entity.TaskViewId;
import io.spine.test.entity.TaskViewWithColumns;

import static io.spine.test.entity.TaskView.Status.CREATED;

public final class SimpleColumn {

    /** Prevents instantiation of this test env class. */
    private SimpleColumn() {
    }

    public static Column column() {
        return stringColumn();
    }

    public static Column stringColumn() {
        return column("name");
    }

    public static Column floatColumn() {
        return column("estimate_in_days");
    }

    public static Column timestampColumn() {
        return column("due_date");
    }

    private static Column column(String name) {
        Columns columns = Columns.of(TaskViewProjection.class);
        ColumnName columnName = ColumnName.of(name);
        Column column = columns.get(columnName);
        return column;
    }

    private static class TaskViewProjection
            extends Projection<TaskViewId, TaskView, TaskView.Builder>
            implements TaskViewWithColumns {

        @Override
        public String getName() {
            return "some-name";
        }

        @Override
        public float getEstimateInDays() {
            return 42.0F;
        }

        @Override
        public TaskView.Status getStatus() {
            return CREATED;
        }

        @Override
        public Timestamp getDueDate() {
            return Timestamp.getDefaultInstance();
        }
    }
}
