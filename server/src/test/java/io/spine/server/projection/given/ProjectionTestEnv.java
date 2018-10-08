/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.projection.given;

import com.google.protobuf.StringValue;
import io.spine.core.ByField;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.projection.event.Int32Imported;
import io.spine.test.projection.event.PairImported;
import io.spine.test.projection.event.StringImported;
import io.spine.validate.StringValueVBuilder;

import static io.spine.protobuf.TypeConverter.toMessage;
import static org.junit.jupiter.api.Assertions.fail;

public class ProjectionTestEnv {

    private static final String VALUE_FIELD_PATH = "value";

    /** Prevents instantiation of this utility class. */
    private ProjectionTestEnv() {
    }

    public static final class TestProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

        /** The number of events this class handles. */
        public static final int HANDLING_EVENT_COUNT = 2;

        private TestProjection(String id) {
            super(id);
        }

        @Subscribe
        public void on(StringImported event) {
            StringValue newState = createNewState("stringState", event.getValue());
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void on(Int32Imported event) {
            StringValue newState = createNewState("integerState",
                                                  String.valueOf(event.getValue()));
            getBuilder().mergeFrom(newState);
        }

        private StringValue createNewState(String type, String value) {
            // Get the current state within the transaction.
            String currentState = getBuilder().internalBuild()
                                              .getValue();
            String result = currentState + (currentState.length() > 0 ? " + " : "") +
                    type + '(' + value + ')' + System.lineSeparator();
            return toMessage(result);
        }
    }

    public static final class FilteringProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

        public static final String SET_A = "ONE_UNDER_SCOPE";

        public static final String SET_B = "TWO_UNDER_SCOPES";

        private FilteringProjection(String id) {
            super(id);
        }

        @Subscribe(filter = @ByField(path = VALUE_FIELD_PATH, value = SET_A))
        public void onReserved(StringImported event) {
            getBuilder().setValue("A");
        }

        @Subscribe(filter = @ByField(path = VALUE_FIELD_PATH, value = SET_B))
        public void onSecret(StringImported event) {
            getBuilder().setValue("B");
        }

        @Subscribe
        public void on(StringImported event) {
            getBuilder().setValue(event.getValue());
        }
    }

    public static final class NoDefaultOptionProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

        public static final String ACCEPTED_VALUE = "AAA";

        private NoDefaultOptionProjection(String id) {
            super(id);
        }

        @Subscribe(filter = @ByField(path = VALUE_FIELD_PATH, value = ACCEPTED_VALUE))
        public void on(StringImported event) {
            getBuilder().setValue(event.getValue());
        }
    }

    public static final class MalformedProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

        private MalformedProjection(String id) {
            super(id);
        }

        @Subscribe(filter = @ByField(path = "integer", value = "42"))
        public void onInt(PairImported event) {
            halt();
        }

        @Subscribe(filter = @ByField(path = "str", value = "42"))
        public void onString(PairImported event) {
            halt();
        }

        public static final class Repository
                extends ProjectionRepository<String, MalformedProjection, StringValue> {
        }
    }

    public static final class DuplicateFilterProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

        private DuplicateFilterProjection(String id) {
            super(id);
        }

        @Subscribe(filter = @ByField(path = VALUE_FIELD_PATH, value = "1"))
        public void onString1(Int32Imported event) {
            halt();
        }

        @Subscribe(filter = @ByField(path = VALUE_FIELD_PATH, value = "+1"))
        public void onStringOne(Int32Imported event) {
            halt();
        }

        public static final class Repository
                extends ProjectionRepository<String, DuplicateFilterProjection, StringValue> {
        }
    }

    private static void halt() {
        fail("Should never be called.");
    }
}
