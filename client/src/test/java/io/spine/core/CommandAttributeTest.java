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

package io.spine.core;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.base.Time;
import io.spine.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.TypeConverter.toMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@linkplain CommandAttribute CommandAttribute API}.
 *
 * <p>The test suite is located under the "client" module since actor request generation
 * is required. So we want to avoid circular dependencies between "core" and "client" modules.
 *
 * @author Alexander Yevsyukov
 */
@DisplayName("Command attribute should")
class CommandAttributeTest {

    private final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(CommandAttributeTest.class);

    private CommandContext.Builder contextBuilder;

    @BeforeEach
    void setUp() {
        Command command = factory.createCommand(Empty.getDefaultInstance(),
                                                Time.getCurrentTime());
        contextBuilder = command.getContext()
                                .toBuilder();
    }

    private <T> void assertSetGet(CommandAttribute<T> attr, T value) {
        attr.setValue(contextBuilder, value);

        assertEquals(value, attr.getValue(contextBuilder.build())
                                .get());
    }

    @Test
    @DisplayName("set and get bool attribute value")
    void setAndGetBool() {
        CommandAttribute<Boolean> attr = new CommandAttribute<Boolean>("flag") {
        };
        assertSetGet(attr, true);
        assertSetGet(attr, false);
    }

    @Test
    @DisplayName("set and get string attribute value")
    void setAndGetString() {
        CommandAttribute<String> attr = new CommandAttribute<String>("str") {
        };
        String value = getClass().getName();

        assertSetGet(attr, value);
    }

    @Test
    @DisplayName("set and get long attribute value")
    void setAndGetLong() {
        CommandAttribute<Long> attr = new CommandAttribute<Long>("l-o-n-g") {
        };
        Long value = 10101010L;

        assertSetGet(attr, value);
    }

    @Test
    @DisplayName("set and get int attribute value")
    void setAndGetInt() {
        CommandAttribute<Integer> attr = new CommandAttribute<Integer>("int") {
        };
        Integer value = 1024;

        assertSetGet(attr, value);
    }

    @Test
    @DisplayName("set and get protobuf message attribute value")
    void setAndGetMessage() {
        CommandAttribute<StringValue> attr = new CommandAttribute<StringValue>("str-val") {
        };
        StringValue value = toMessage(getClass().getName());

        assertSetGet(attr, value);
    }

    @Test
    @DisplayName("set and get float attribute value")
    void setAndGetFloat() {
        CommandAttribute<Float> attr = new CommandAttribute<Float>("flp") {
        };
        Float value = 1024.512f;

        assertSetGet(attr, value);
    }

    @Test
    @DisplayName("set and get double attribute value")
    void setAndGetDouble() {
        CommandAttribute<Double> attr = new CommandAttribute<Double>("dbl") {
        };
        Double value = 100.500;

        assertSetGet(attr, value);
    }

    @Test
    @DisplayName("fail on setting unsupported value type")
    void fail_on_unsupported_type() {
        CommandAttribute<Object> attr = new CommandAttribute<Object>("o") {
        };

        @SuppressWarnings("EmptyClass") Object value = new Object() {
        };

        assertThrows(IllegalArgumentException.class, () -> attr.setValue(contextBuilder, value));
    }
}
