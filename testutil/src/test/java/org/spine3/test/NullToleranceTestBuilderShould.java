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

package org.spine3.test;

import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Illia Shepilov
 */
public class NullToleranceTestBuilderShould {

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions") // The builder methods are tested with {@code null} argument values.
    public void not_accept_null_target_class() {
        NullToleranceTest.newBuilder()
                         .setClass(null);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions") // The builder methods are tested with {@code null} argument values.
    public void throw_exception_when_pass_null_as_excluded_method_name() {
        NullToleranceTest.newBuilder()
                         .excludeMethod(null);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions") // The builder methods are tested with {@code null} argument values.
    public void throw_exception_when_pass_null_as_default_value() {
        NullToleranceTest.newBuilder()
                         .addDefaultValue(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_pass_empty_string_as_excluded_method_name() {
        NullToleranceTest.newBuilder()
                         .excludeMethod("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_pass_invalid_excluded_method_name() {
        NullToleranceTest.newBuilder()
                         .excludeMethod("illegal method name");
    }

    @Test
    public void return_target_class() {
        final Class<?> targetClass = NullToleranceTest.newBuilder()
                                                      .setClass(NullToleranceTestBuilderShould.class)
                                                      .getTargetClass();
        assertEquals(NullToleranceTestBuilderShould.class, targetClass);
    }

    @Test
    public void return_empty_default_values() {
        final NullToleranceTest.Builder builder = NullToleranceTest.newBuilder();

        assertTrue(builder.getDefaultValues()
                          .isEmpty());
    }

    @Test
    public void return_default_values_map() {
        final NullToleranceTest.Builder builder = NullToleranceTest.newBuilder();

        final int integerDefaultValue = 0;
        builder.addDefaultValue(integerDefaultValue);
        final int actualSize = builder.getDefaultValues()
                                      .size();
        assertEquals(1, actualSize);

        final Map<? super Class, ? super Object> expectedMap = newHashMap();
        expectedMap.put(Integer.class, integerDefaultValue);
        assertEquals(expectedMap, builder.getDefaultValues());
    }

    @Test
    public void return_excluded_method_names_set() {
        final NullToleranceTest.Builder builder = NullToleranceTest.newBuilder();

        final String firstMethodName = "method";
        final String secondMethodName = "secondMethod";
        builder.excludeMethod(firstMethodName);
        builder.excludeMethod(secondMethodName);

        final int actualSize = builder.getExcludedMethods()
                                      .size();
        assertEquals(2, actualSize);

        final Set<String> expectedMethodNames = newHashSet(firstMethodName, secondMethodName);
        final Set<String> actualMethodNames = builder.getExcludedMethods();
        assertEquals(expectedMethodNames, actualMethodNames);
    }

    @Test(expected = IllegalStateException.class)
    public void require_target_class_to_build_instance() {
        NullToleranceTest.newBuilder()
                         .build();
    }

    @Test
    public void build_instance_when_all_required_fields_are_set() {
        final String excludedMethodName = "excludedMethod";
        final NullToleranceTestBuilderShould instance = new NullToleranceTestBuilderShould();
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(NullToleranceTestBuilderShould.class)
                                                                     .addDefaultValue(instance)
                                                                     .excludeMethod(excludedMethodName)
                                                                     .build();
        final Map<? super Class, ? super Object> expectedMap = newHashMap();
        expectedMap.put(NullToleranceTestBuilderShould.class, instance);
        expectedMap.put(String.class, "");
        assertEquals(NullToleranceTestBuilderShould.class, nullToleranceTest.getTargetClass());
        assertEquals(newHashSet(excludedMethodName), nullToleranceTest.getExcludedMethods());
        assertEquals(expectedMap, nullToleranceTest.getDefaultValues());
    }

    @Test
    public void build_instance_with_provided_default_value_for_the_string_class() {
        final String defaultValue = "default value";
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(NullToleranceTestBuilderShould.class)
                                                                     .addDefaultValue(defaultValue)
                                                                     .build();
        final Collection<?> defaultValues = nullToleranceTest.getDefaultValues()
                                                             .values();
        assertEquals(1, defaultValues.size());
        final boolean contains = defaultValues.contains(defaultValue);
        assertTrue(contains);
    }
}
