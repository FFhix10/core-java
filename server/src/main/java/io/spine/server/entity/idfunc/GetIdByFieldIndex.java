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

package io.spine.server.entity.idfunc;

import com.google.protobuf.Message;
import io.spine.base.EventContext;
import io.spine.base.Identifier;
import io.spine.protobuf.MessageField;

/**
 * Obtains an entity ID based on an event/command message, context and message field index.
 *
 * <p>An entity ID field name must end with the {@link Identifier#ID_PROPERTY_SUFFIX}.
 *
 * @param <I> the type of entity IDs
 * @param <M> the type of messages to get IDs from
 * @param <C> either {@link EventContext EventContext} or
 *            {@link io.spine.base.CommandContext CommandContext} type
 */
abstract class GetIdByFieldIndex<I, M extends Message, C extends Message>
         implements IdFunction<I, M, C> {

    private static final long serialVersionUID = 0L;
    private final EntityIdField idField;

    /**
     * Creates a new instance.
     *
     * @param idIndex a zero-based index of an ID field in this type of messages
     */
    GetIdByFieldIndex(int idIndex) {
        this.idField = new EntityIdField(idIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @throws MissingEntityIdException if the field name does not end with
     *          the {@link Identifier#ID_PROPERTY_SUFFIX}.
     * @throws ClassCastException if the field type is invalid
     */
    @Override
    public I apply(M message, C context) throws MissingEntityIdException {
        @SuppressWarnings("unchecked") // we expect that the field is of this type
        final I id = (I) idField.getValue(message);
        return id;
    }

    /** Accessor object for entity ID fields. */
    private static class EntityIdField extends MessageField {

        private static final long serialVersionUID = 0L;

        private EntityIdField(int index) {
            super(index);
        }

        @Override
        protected RuntimeException createUnavailableFieldException(Message message,
                                                                   String fieldName) {
            return new MissingEntityIdException(message.getClass().getName(),
                                                fieldName, getIndex());
        }

        @Override
        protected boolean isFieldAvailable(Message message) {
            final String fieldName = MessageField.getFieldName(message, getIndex());
            final boolean result = fieldName.endsWith(Identifier.ID_PROPERTY_SUFFIX);
            return result;
        }
    }
}