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

package org.spine3.protobuf;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Api;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.EnumValue;
import com.google.protobuf.Field;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FloatValue;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Method;
import com.google.protobuf.Mixin;
import com.google.protobuf.Option;
import com.google.protobuf.SourceContext;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Type;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.Internal;
import org.spine3.protobuf.error.UnknownTypeException;
import org.spine3.type.ClassName;

import java.util.Properties;
import java.util.Set;

import static org.spine3.io.IoUtil.loadAllProperties;

/**
 * A map which contains all Protobuf types known to the application.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 */
@Internal
public class KnownTypes {

    private static final char CLASS_PACKAGE_DELIMITER = '.';

    /**
     * File, containing Protobuf messages' typeUrls and their appropriate class names.
     *
     * <p>Is generated by Gradle during build process.
     */
    private static final String PROPS_FILE_PATH = "known_types.properties";

    /**
     * A map from Protobuf type name to Java class name.
     *
     * <p>For example, for a key {@code spine.base.EventId}, there will be the value {@code org.spine3.base.EventId}.
     */
    private static final BiMap<TypeUrl, ClassName> typeToClassMap = Builder.build();

    /**
     * A map from Protobuf type name to type URL.
     *
     * <p>For example, for a key {@code spine.base.EventId},
     * there will be the value {@code type.spine3.org/spine.base.EventId}.
     *
     * @see TypeUrl
     */
    private static final ImmutableMap<String, TypeUrl> typeNameToUrlMap = buildTypeToUrlMap(typeToClassMap);


    private KnownTypes() {}

    /**
     * Retrieves Protobuf types known to the application.
     *
     * @return immutable set of Protobuf types known to the application
     */
    public static ImmutableSet<TypeUrl> typeNames() {
        final Set<TypeUrl> result = typeToClassMap.keySet();
        return ImmutableSet.copyOf(result);
    }

    /**
     * Retrieves compiled proto's java class name by proto type url
     * to be used to parse {@link Message} from {@link Any}.
     *
     * @param protoType {@link Any} type url
     * @return Java class name
     * @throws UnknownTypeException if there is no such type known to the application
     */
    public static ClassName getClassName(TypeUrl protoType) throws UnknownTypeException {
        if (!typeToClassMap.containsKey(protoType)) {
            final ClassName className = findInnerMessageClass(protoType);
            typeToClassMap.put(protoType, className);
        }
        final ClassName result = typeToClassMap.get(protoType);
        return result;
    }

    /**
     * Returns the Protobuf name for the class with the given name.
     *
     * @param className the name of the Java class for which to get Protobuf type
     * @return a Protobuf type name
     * @throws IllegalStateException if there is no Protobuf type for the specified class
     */
    public static TypeUrl getTypeUrl(ClassName className) {
        final TypeUrl result = typeToClassMap.inverse().get(className);
        if (result == null) {
            throw new IllegalStateException("No Protobuf type found for the Java class " + className);
        }
        return result;
    }

    /**
     * Attempts to find a {@link ClassName} for the passed inner Protobuf type.
     *
     * <p>For example, com.package.OuterClass.InnerClass class name.
     *
     * @param typeUrl {@link TypeUrl} of the class to find
     * @return the found class name
     * @throws UnknownTypeException if there is no such type known to the application
     */ // TODO:2016-07-08:alexander.litus: check if it is needed
    private static ClassName findInnerMessageClass(TypeUrl typeUrl) throws UnknownTypeException {
        String lookupType = typeUrl.getTypeName();
        ClassName className = null;
        final StringBuilder suffix = new StringBuilder(lookupType.length());
        int lastDotPosition = lookupType.lastIndexOf(CLASS_PACKAGE_DELIMITER);
        while (className == null && lastDotPosition != -1) {
            suffix.insert(0, lookupType.substring(lastDotPosition));
            lookupType = lookupType.substring(0, lastDotPosition);
            className = typeToClassMap.get(TypeUrl.of(lookupType));
            lastDotPosition = lookupType.lastIndexOf(CLASS_PACKAGE_DELIMITER);
        }
        if (className == null) {
            throw new UnknownTypeException(typeUrl.getTypeName());
        }
        className = ClassName.of(className.value() + suffix);
        try {
            Class.forName(className.value());
        } catch (ClassNotFoundException e) {
            throw new UnknownTypeException(typeUrl.getTypeName(), e);
        }
        return className;
    }

    /** Returns a Protobuf type URL by Protobuf type name. */
    public static TypeUrl getTypeUrl(String typeName) {
        final TypeUrl typeUrl = typeNameToUrlMap.get(typeName);
        return typeUrl;
    }

    private static ImmutableMap<String, TypeUrl> buildTypeToUrlMap(BiMap<TypeUrl, ClassName> typeToClassMap) {
        final ImmutableMap.Builder<String, TypeUrl> builder = ImmutableMap.builder();
        for (TypeUrl typeUrl : typeToClassMap.keySet()) {
            builder.put(typeUrl.getTypeName(), typeUrl);
        }
        return builder.build();
    }

    /**
     * The helper class for building internal immutable type-to-class map.
     */
    private static class Builder {

        private final ImmutableBiMap.Builder<TypeUrl, ClassName> mapBuilder = new ImmutableBiMap.Builder<>();

        private static BiMap<TypeUrl, ClassName> build() {
            final Builder builder = new Builder()
                    .addStandardProtobufTypes()
                    .loadNamesFromProperties();

            final ImmutableBiMap<TypeUrl, ClassName> result = builder.mapBuilder.build();

            if (log().isDebugEnabled()) {
                log().debug("Total classes in TypeToClassMap: " + result.size());
            }

            return result;
        }

        private Builder loadNamesFromProperties() {
            final Set<Properties> propertiesSet = loadAllProperties(PROPS_FILE_PATH);
            for (Properties properties : propertiesSet) {
                putProperties(properties);
            }
            return this;
        }

        private Builder putProperties(Properties properties) {
            final Set<String> typeUrls = properties.stringPropertyNames();
            for (String typeUrlStr : typeUrls) {
                final TypeUrl typeUrl = TypeUrl.of(typeUrlStr);
                final ClassName className = ClassName.of(properties.getProperty(typeUrlStr));
                mapBuilder.put(typeUrl, className);
            }
            return this;
        }

        /**
         * Returns classes from the {@code com.google.protobuf} package that need to be present
         * in the type-to-class map.
         *
         * <p>This method needs to be updated with introduction of new Google Protobuf types
         * after they are used in the framework.
         */
        @SuppressWarnings("OverlyLongMethod") // OK as there are many types in Protobuf.
        private Builder addStandardProtobufTypes() {
            // Types from `any.proto`.
            put(Any.class);

            // Types from `api.proto`
            put(Api.class);
            put(Method.class);
            put(Mixin.class);

            // Types from `descriptor.proto`
            put(DescriptorProtos.FileDescriptorSet.class);
            put(DescriptorProtos.FileDescriptorProto.class);
            put(DescriptorProtos.DescriptorProto.class);
                // Inner types of `DescriptorProto`
                put(DescriptorProtos.DescriptorProto.ExtensionRange.class);
                put(DescriptorProtos.DescriptorProto.ReservedRange.class);

            put(DescriptorProtos.FieldDescriptorProto.class);
                //TODO:2016-07-07:alexander.yevsyukov: Handle internal enum Type
                //TODO:2016-07-07:alexander.yevsyukov: Handle internal enum Label

            put(DescriptorProtos.OneofDescriptorProto.class);
            put(DescriptorProtos.EnumDescriptorProto.class);
            put(DescriptorProtos.EnumValueDescriptorProto.class);
            put(DescriptorProtos.ServiceDescriptorProto.class);
            put(DescriptorProtos.MethodDescriptorProto.class);
            put(DescriptorProtos.FileOptions.class);
                //TODO:2016-07-07:alexander.yevsyukov: Handle enum OptimizeMode
            put(DescriptorProtos.MessageOptions.class);
            put(DescriptorProtos.FieldOptions.class);
                //TODO:2016-07-07:alexander.yevsyukov: Handle enum CType
                //TODO:2016-07-07:alexander.yevsyukov: Handle enum JSType
            put(DescriptorProtos.EnumOptions.class);
            put(DescriptorProtos.EnumValueOptions.class);
            put(DescriptorProtos.ServiceOptions.class);
            put(DescriptorProtos.MethodOptions.class);
            put(DescriptorProtos.UninterpretedOption.class);
            put(DescriptorProtos.SourceCodeInfo.class);
                // Inner types of `SourceCodeInfo`.
                put(DescriptorProtos.SourceCodeInfo.Location.class);
            put(DescriptorProtos.GeneratedCodeInfo.class);
                // Inner types of `GeneratedCodeInfo`.
                put(DescriptorProtos.GeneratedCodeInfo.Annotation.class);

            // Types from `duration.proto`.
            put(Duration.class);

            // Types from `empty.proto`.
            put(Empty.class);

            // Types from `field_mask.proto`.
            put(FieldMask.class);

            // Types from `source_context.proto`.
            put(SourceContext.class);

            // Types from `struct.proto`.
            put(Struct.class);
            put(Value.class);
                //TODO:2016-07-07:alexander.yevsyukov: handle enum NullValue
            put(ListValue.class);

            // Types from `timestamp.proto`.
            put(Timestamp.class);

            // Types from `type.proto`.
            put(Type.class);
            put(Field.class);
                //TODO:2016-07-07:alexander.yevsyukov: Handle enum Kind.
                //TODO:2016-07-07:alexander.yevsyukov: Handle enum Cardinality
            put(com.google.protobuf.Enum.class);
            put(EnumValue.class);
            put(Option.class);
                //TODO:2016-07-07:alexander.yevsyukov: Handle enum Syntax

            // Types from `wrappers.proto`.
            put(DoubleValue.class);
            put(FloatValue.class);
            put(Int64Value.class);
            put(UInt64Value.class);
            put(Int32Value.class);
            put(UInt32Value.class);
            put(BoolValue.class);
            put(StringValue.class);
            put(BytesValue.class);

            return this;
        }

        private void put(Class<? extends GeneratedMessage> clazz) {
            final TypeUrl typeUrl = TypeUrl.of(clazz);
            final ClassName className = ClassName.of(clazz);
            mapBuilder.put(typeUrl, className);
        }

        private static Logger log() {
            return LogSingleton.INSTANCE.value;
        }

        private enum LogSingleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Logger value = LoggerFactory.getLogger(KnownTypes.class);
        }
    }
}
