/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.type;

import com.google.protobuf.Any;
import com.google.protobuf.AnyOrBuilder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.validate.Validate;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * A value object for fully-qualified Protobuf type name.
 *
 * @see Descriptors.FileDescriptor#getFullName()
 * @see Message#getDescriptorForType()
 * @author Alexander Yevsyukov
 */
public final class TypeName extends StringTypeValue {

    private TypeName(String value) {
        super(checkNotEmpty(value));
    }

    private TypeName(MessageOrBuilder msg) {
        this(msg.getDescriptorForType().getFullName());
    }

    /**
     * Creates a new type name instance taking its name from the passed message instance.
     * @param msg an instance to get the type name from
     * @return new instance
     */
    public static TypeName of(Message msg) {
        return new TypeName(msg);
    }

    /**
     * Creates a new instance by the passed descriptor taking full name of the type.
     *
     * @param descriptor the descriptor of the type
     * @return new instance
     */
    public static TypeName of(Descriptors.Descriptor descriptor) {
        return new TypeName(descriptor.getFullName());
    }

    /**
     * Creates a new instance with the passed type name.
     *
     * @param typeName the name of the type
     * @return new instance
     */
    @Internal
    public static TypeName of(String typeName) {
        return new TypeName(typeName);
    }

    /**
     * Obtains the type name of the message enclosed into passed instance of {@link Any}.
     *
     * <p>The type name is the string coming after "type.googleapis.com/" in the URL
     * returned by {@link Any#getTypeUrl()}.
     *
     * @param any the instance of {@code Any} containing {@code Message} instance of interest
     * @return new instance of {@code TypeName}
     */
    public static TypeName ofEnclosed(AnyOrBuilder any) {
        String typeName = null;
        try {
            typeName = getTypeName(any.getTypeUrl());
        } catch (InvalidProtocolBufferException e) {
            propagate(e);
        }
        assert typeName != null;
        return of(typeName);
    }

    /**
     * Obtains the type name of the command's message.
     *
     * <p>The passed command must have non-default message.
     *
     * @param command the command to inspect
     * @return the type name of the command message
     */
    public static TypeName ofCommand(Command command) {
        final Any message = command.getMessage();
        Validate.checkNotDefault(message);

        final TypeName commandType = ofEnclosed(message);
        return commandType;
    }

    /**
     * Obtains type name for the passed message class.
     */
    public static TypeName of(Class<? extends Message> clazz) {
        final Message defaultInstance = com.google.protobuf.Internal.getDefaultInstance(clazz);
        final TypeName result = of(defaultInstance);
        return result;
    }

    private static final String TYPE_URL_PREFIX = "type.googleapis.com";

    private static final String TYPE_URL_SEPARATOR = "/";
    private static final Pattern TYPE_URL_SEPARATOR_PATTERN = Pattern.compile(TYPE_URL_SEPARATOR);
    private static final Pattern PROTOBUF_PACKAGE_SEPARATOR = Pattern.compile("\\.");

    private static String getTypeName(String typeUrl)
            throws InvalidProtocolBufferException {
        final String[] parts = TYPE_URL_SEPARATOR_PATTERN.split(typeUrl);
        if (parts.length != 2 || !parts[0].equals(TYPE_URL_PREFIX)) {
            throw new InvalidProtocolBufferException(
                    "Invalid type url encountered: " + typeUrl);
        }
        return parts[1];
    }

    //TODO:2016-07-07:alexander.yevsyukov: Support custom type URLs in this method.
    /**
     * Returns string to be used as a type URL in {@code Any}.
     *
     * @return string with "type.googleapis.com/" prefix followed by the full type name
     * @see Any#getTypeUrl()
     * @see #ofEnclosed(AnyOrBuilder)
     */
    public String toTypeUrl() {
        final String result = TYPE_URL_PREFIX + TYPE_URL_SEPARATOR + value();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value() {
        // Expose method to other packages.
        return super.value();
    }

    /**
     * Returns the name part of the fully qualified name.
     *
     * <p>If there's no package part in the type name, its value is returned.
     *
     * @return string with the type name
     */
    public String nameOnly() {
        final String value = value();
        final String[] parts = PROTOBUF_PACKAGE_SEPARATOR.split(value);
        if (parts.length < 2) {
            // There's only one element in the array since there's no separator found.
            // If this is so, the type name has no package.
            return value;
        }
        final String result = parts[parts.length - 1];
        return result;
    }

    private static String checkNotEmpty(String value) {
        checkNotNull(value, "TypeName cannot be null.") ;
        checkArgument(!value.isEmpty(), "TypeName cannot be empty string.");
        return value;
    }
}
