/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.bus;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Rejection;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.bus.given.BusesTestEnv.Exceptions.DeadMessageException;
import io.spine.server.bus.given.BusesTestEnv.Exceptions.FailedValidationException;
import io.spine.server.bus.given.BusesTestEnv.Exceptions.FailingFilterException;
import io.spine.server.bus.given.BusesTestEnv.Filters.FailingFilter;
import io.spine.server.bus.given.BusesTestEnv.Filters.PassingFilter;
import io.spine.server.bus.given.BusesTestEnv.TestMessageBus;
import io.spine.server.bus.given.BusesTestEnv.TestMessageContentsDispatcher;
import io.spine.test.bus.BusMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.bus.given.BusesTestEnv.STATUS_OK;
import static io.spine.server.bus.given.BusesTestEnv.busMessage;
import static io.spine.server.bus.given.BusesTestEnv.errorType;
import static io.spine.server.bus.given.BusesTestEnv.testContents;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class BusesShould {

    private TestMessageBus bus;
    private TestMessageBus deadBus;
    private TestMessageBus invalidatableBus;
    private TestMessageBus invalidatableDeadBus;

    @Before
    public void setUp() {
        deadBus = TestMessageBus.newInstance();

        final TestMessageContentsDispatcher dispatcher = new TestMessageContentsDispatcher();

        bus = TestMessageBus.newInstance();
        bus.register(dispatcher);

        invalidatableDeadBus = TestMessageBus.newInstance();
        invalidatableDeadBus.setInvalidValidator();

        invalidatableBus = TestMessageBus.newInstance();
        invalidatableBus.setInvalidValidator();
        invalidatableBus.register(dispatcher);
    }

    @After
    public void tearDown() throws Exception {
        bus.close();
        deadBus.close();
        invalidatableBus.close();
        invalidatableDeadBus.close();
    }

    @Test
    public void have_private_util_ctor() {
        assertHasPrivateParameterlessCtor(Buses.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(Message.class, Any.getDefaultInstance())
                .setDefault(Error.class, Error.newBuilder()
                                              .setCode(1)
                                              .build())
                .setDefault(Rejection.class, Rejection.newBuilder()
                                                      .setMessage(Any.getDefaultInstance())
                                                      .build())
                .testAllPublicStaticMethods(Buses.class);
    }

    @Test
    public void deliver_a_valid_message_with_registered_dispatcher() {
        final BusMessage message = busMessage(testContents());
        final MemoizingObserver<Ack> observer = memoizingObserver();

        bus.post(message, observer);

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        assertEquals(STATUS_OK, response.getStatus());
    }

    @Test
    public void apply_the_validating_filter_prior_to_the_dead_message_filter() {
        testBusForError(invalidatableDeadBus, FailedValidationException.TYPE);
    }

    @Test
    public void apply_registered_filters_prior_to_the_validating_filter() {
        invalidatableDeadBus.register(new FailingFilter());

        testBusForError(invalidatableDeadBus, FailingFilterException.TYPE);
    }

    @Test
    public void apply_the_validating_filter() {
        testBusForError(invalidatableBus, FailedValidationException.TYPE);
    }

    @Test
    public void apply_a_registered_filter() {
        bus.register(new FailingFilter());

        testBusForError(bus, FailingFilterException.TYPE);
    }

    @Test
    public void apply_registered_filters() {
        final PassingFilter filter1 = new PassingFilter();
        final PassingFilter filter2 = new PassingFilter();
        bus.register(filter1);
        bus.register(filter2);
        bus.register(new FailingFilter());

        testBusForError(bus, FailingFilterException.TYPE);

        assertTrue(filter1.passed());
        assertTrue(filter2.passed());
    }

    @Test
    public void apply_the_dead_message_filter() {
        testBusForError(deadBus, DeadMessageException.TYPE);
    }

    private static void testBusForError(TestMessageBus invalidatableDeadBus, String type) {
        final BusMessage message = busMessage(testContents());
        final MemoizingObserver<Ack> observer = memoizingObserver();

        invalidatableDeadBus.post(message, observer);

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        assertEquals(type, errorType(response));
        assertSize(0, invalidatableDeadBus.storedMessages());
    }

}
