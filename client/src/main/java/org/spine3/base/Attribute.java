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

package org.spine3.base;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Wrapper;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An attribute stored in a protobuf {@code map<string, Any>}.
 *
 * @param <T> the type of the attribute value
 * @param <M> the type of the message object to which the attribute belongs
 * @param <B> the type of the message builder
 *
 * @author Alexander Yevsyukov
 */
public abstract class Attribute<T, M extends Message, B extends Message.Builder> {

    private final Type type;
    private final String name;

    protected Attribute(Type type, String name) {
        checkNotNull(type);
        checkNotNull(name);
        checkArgument(name.length() > 0, "Attribute name cannot be empty");
        this.type = type;
        this.name = name;
    }

    /**
     * Obtains attribute map from the enclosing object.
     */
    protected abstract Map<String, Any> getMap(M obj);

    protected abstract Map<String, Any> getMutableMap(B builder);

    /**
     * Extracts the value from {@code Any}.
     */
    protected T unpack(Any any) {
        final T result = type.unpack(any);
        return result;
    }

    protected Any pack(T value) {
        final Any result = type.pack(value);
        return result;
    }

    private Optional<T> getValue(Map<String, Any> map) {
        final Any any = map.get(name);
        if (any == null || Any.getDefaultInstance()
                              .equals(any)) {
            return Optional.absent();
        }

        final T result = unpack(any);
        return Optional.of(result);
    }

    public Optional<T> get(M obj) {
        final Map<String, Any> map = getMap(obj);
        final Optional<T> result = getValue(map);
        return result;
    }

    public void set(B builder, T value) {
        final Map<String, Any> map = getMutableMap(builder);
        final Any packed = this.pack(value);
        map.put(name, packed);
    }

    @SuppressWarnings("unchecked") // Conversions are safe as we unpack specific types.
    protected enum Type {
        BOOLEAN {
            @Override
            <T> Any pack(T value) {
                return Wrapper.forBoolean()
                              .pack((Boolean) value);
            }

            @Override
            Boolean unpack(Any any) {
                return Wrapper.forBoolean()
                              .unpack(any);
            }
        },

        STRING {
            @Override
            <T> Any pack(T value) {
                return Wrapper.forString()
                              .pack((String) value);
            }

            @Override
            String unpack(Any any) {
                return Wrapper.forString()
                              .unpack(any);
            }
        },

        INTEGER {
            @Override
            Integer unpack(Any any) {
                return Wrapper.forInteger()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forInteger()
                              .pack((Integer) value);
            }
        },

        LONG {
            @Override
            Long unpack(Any any) {
                return Wrapper.forLong()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forLong()
                              .pack((Long) value);
            }
        },

        FLOAT {
            @Override
            Float unpack(Any any) {
                return Wrapper.forFloat()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forFloat()
                              .pack((Float) value);
            }
        },

        DOUBLE {
            @Override
            Double unpack(Any any) {
                return Wrapper.forDouble()
                              .unpack(any);
            }

            @Override
            <T> Any pack(T value) {
                return Wrapper.forDouble()
                              .pack((Double) value);
            }
        },

        MESSAGE {
            @Override
            <T> T unpack(Any any) {
                final T result = AnyPacker.unpack(any);
                return result;
            }

            @Override
            <T> Any pack(T value) {
                return AnyPacker.pack((Message) value);
            }
        };

        abstract <T> T unpack(Any any);

        abstract <T> Any pack(T value);
    }
}
