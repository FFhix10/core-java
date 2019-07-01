/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.trace;

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.server.ContextSpec;
import io.spine.server.ServerEnvironment;
import io.spine.server.trace.given.MemoizingTracer;
import io.spine.server.trace.given.MemoizingTracerFactory;
import io.spine.server.trace.given.airport.AirportContext;
import io.spine.test.trace.BoardingCanceled;
import io.spine.test.trace.BoardingStarted;
import io.spine.test.trace.CancelBoarding;
import io.spine.test.trace.CancelFlight;
import io.spine.test.trace.FlightCanceled;
import io.spine.test.trace.FlightScheduled;
import io.spine.test.trace.ScheduleFlight;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.trace.given.TracingTestEnv.BOARDING_TYPE;
import static io.spine.server.trace.given.TracingTestEnv.FLIGHT;
import static io.spine.server.trace.given.TracingTestEnv.FLIGHT_TYPE;
import static io.spine.server.trace.given.TracingTestEnv.FROM;
import static io.spine.server.trace.given.TracingTestEnv.TIMETABLE_TYPE;
import static io.spine.server.trace.given.TracingTestEnv.cancelFlight;
import static io.spine.server.trace.given.TracingTestEnv.scheduleFlight;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Message tracing should")
class TracingTest {

    private static final ServerEnvironment serverEnvironment = ServerEnvironment.instance();
    private static final TestActorRequestFactory requests =
            new TestActorRequestFactory(TracingTest.class);

    private MemoizingTracerFactory tracing;
    private BoundedContext context;
    private ContextSpec spec;

    @BeforeEach
    void setUp() {
        tracing = new MemoizingTracerFactory();
        serverEnvironment.configureTracing(tracing);
        context = AirportContext
                .builder()
                .build();
        spec = context.spec();
    }

    @AfterEach
    void tearDown() {
        serverEnvironment.reset();
    }
    @Test
    @DisplayName("trace actor commands")
    void traceCommands() {
        post(scheduleFlight());
        MemoizingTracer tracer = tracing.tracer(spec, ScheduleFlight.class);
        assertTrue(tracer.isReceiver(FLIGHT, FLIGHT_TYPE));
    }

    @Test
    @DisplayName("trace many messages generated by a single actor message")
    void traceComplexMessageTrees() {
        post(scheduleFlight());

        assertReceiver(ScheduleFlight.class, FLIGHT, FLIGHT_TYPE);
        assertReceiver(FlightScheduled.class, FROM, TIMETABLE_TYPE);
        assertReceiver(FlightScheduled.class, FLIGHT, BOARDING_TYPE);
        assertReceiver(BoardingStarted.class, FLIGHT, BOARDING_TYPE);

        post(cancelFlight());

        assertReceiver(CancelFlight.class, FLIGHT, FLIGHT_TYPE);
        assertReceiver(FlightCanceled.class, FROM, TIMETABLE_TYPE);
        assertReceiver(FlightCanceled.class, FLIGHT, BOARDING_TYPE);
        assertReceiver(CancelBoarding.class, FLIGHT, BOARDING_TYPE);
        assertReceiver(BoardingCanceled.class, FLIGHT, BOARDING_TYPE);
    }

    /**
     * Asserts that the passed message type is received by entity with the passed ID and state type.
     */
    private void assertReceiver(Class<? extends Message> messageType,
                                Message entityId,
                                TypeUrl entityStateType) {
        assertTrue(
                tracing.tracer(spec, messageType).isReceiver(entityId, entityStateType)
        );
    }

    private void post(CommandMessage command) {
        Command cmd = requests.command()
                              .create(command);
        context.commandBus()
               .post(cmd, noOpObserver());
    }
}
