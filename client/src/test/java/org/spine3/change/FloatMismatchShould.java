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

package org.spine3.change;

import org.junit.Test;
import org.spine3.test.NullToleranceTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.change.BooleanMismatch.expectedTrue;
import static org.spine3.change.FloatMismatch.expectedNonZero;
import static org.spine3.change.FloatMismatch.expectedZero;
import static org.spine3.change.FloatMismatch.unexpectedValue;
import static org.spine3.change.FloatMismatch.unpackActual;
import static org.spine3.change.FloatMismatch.unpackExpected;
import static org.spine3.change.FloatMismatch.unpackNewValue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

public class FloatMismatchShould {

    private static final int VERSION = 100;
    private static final float EXPECTED = 18.39f;
    private static final float ACTUAL = 19.01f;
    private static final float NEW_VALUE = 14.52f;
    private static final float DELTA = 0.01f;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(FloatMismatch.class));
    }

    @Test
    public void return_mismatch_object_with_float_values() {
        final ValueMismatch mismatch = FloatMismatch.of(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch), DELTA);
        assertEquals(ACTUAL, unpackActual(mismatch), DELTA);
        assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_expected_zero_amount() {
        final double expected = 0.0f;
        final ValueMismatch mismatch = expectedZero(ACTUAL, NEW_VALUE, VERSION);

        assertEquals(expected, unpackExpected(mismatch), DELTA);
        assertEquals(ACTUAL, unpackActual(mismatch), DELTA);
        assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_expected_non_zero_amount() {
        final float actual = 0.0f;
        final ValueMismatch mismatch = expectedNonZero(EXPECTED, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch), DELTA);
        assertEquals(actual, unpackActual(mismatch), DELTA);
        assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_unexpected_int_value() {
        final ValueMismatch mismatch = unexpectedValue(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch), DELTA);
        assertEquals(ACTUAL, unpackActual(mismatch), DELTA);
        assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackExpected_if_its_not_a_IntMismatch() {
        final ValueMismatch mismatch = expectedTrue(VERSION);
        unpackExpected(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackActual_if_its_not_a_IntMismatch() {
        final ValueMismatch mismatch = expectedTrue(VERSION);
        unpackActual(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackNewValue_if_its_not_a_IntMismatch() {
        final ValueMismatch mismatch = expectedTrue(VERSION);
        unpackNewValue(mismatch);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_same_expected_and_actual() {
        final float value = 19.19f;
        unexpectedValue(value, value, NEW_VALUE, VERSION);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(FloatMismatch.class)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }
}
