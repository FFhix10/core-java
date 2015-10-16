/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.internal.EventHandlerMethod;
import org.spine3.server.Entity;
import org.spine3.util.Classes;
import org.spine3.util.MethodMap;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;

/**
 * Creates data projection from incoming events.
 *
 * @param <I> the type of the IDs of the stateful handlers
 * @param <M> the type of the state objects
 */
public abstract class Projection<I, M extends Message> extends Entity<I, M> {

    private MethodMap handlers;

    protected Projection(I id) {
        super(id);
    }

    /**
     * @return non-null state of the projection
     * @throws IllegalStateException if the state was not set to default by calling {@link #setDefault()}
     *                               upon creation of the new instance
     */
    @Nonnull
    @Override
    public M getState() {
        final M state = super.getState();
        // The state must be set for newly created projections by calling setDefault();
        checkState(state != null, "null projection state. Call setDefault() after creating new instance.");
        return state;
    }

    protected void handle(Message event, EventContext ctx) {
        init();
        dispatch(event, ctx);
    }

    private void dispatch(Message event, EventContext ctx) {
        final Class<? extends Message> eventClass = event.getClass();
        Method method = handlers.get(eventClass);
        if (method == null) {
            throw missingEventHandler(eventClass);
        }
        EventHandlerMethod handler = new EventHandlerMethod(this, method);
        try {
            handler.invoke(event, ctx);
        } catch (InvocationTargetException e) {
            propagate(e);
        }
    }

    protected boolean isInitialized() {
        return handlers != null;
    }

    /**
     * Performs initialization of the instance and registers this class of projections in {@link Registry}
     * if it is not registered yet.
     */
    protected void init() {
        if (!isInitialized()) {
            final Registry registry = Registry.getInstance();
            final Class<? extends Projection> thisClass = getClass();

            if (!registry.contains(thisClass)) {
                registry.register(thisClass);
            }

            handlers = registry.getEventHandlers(thisClass);
        }
    }

    /**
     * Returns the set of event classes handled by the passed projection class.
     *
     * @param clazz the class to inspect
     * @return immutable set of event classes or an empty set if no events are handled
     */
    public static ImmutableSet<Class<? extends Message>> getEventClasses(Class<? extends Projection> clazz) {
        return Classes.getHandledMessageClasses(clazz, EventHandlerMethod.isEventHandlerPredicate);
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        return new IllegalStateException(String.format("Missing event handler for event class %s in the projection class %s",
                eventClass, this.getClass()));
    }

    private static class Registry {
        private final MethodMap.Registry<Projection> eventHandlers = new MethodMap.Registry<>();

        boolean contains(Class<? extends Projection> clazz) {
            return eventHandlers.contains(clazz);
        }

        void register(Class<? extends Projection> clazz) {
            eventHandlers.register(clazz, EventHandlerMethod.isEventHandlerPredicate);
        }

        MethodMap getEventHandlers(Class<? extends Projection> clazz) {
            MethodMap result = eventHandlers.get(clazz);
            return result;
        }

        static Registry getInstance() {
            return RegistrySingleton.INSTANCE.value;
        }

        @SuppressWarnings("InnerClassTooDeeplyNested")
        private enum RegistrySingleton {
            INSTANCE;

            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Registry value = new Registry();
        }
    }
}