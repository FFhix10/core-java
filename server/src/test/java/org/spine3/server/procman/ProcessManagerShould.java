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

package org.spine3.server.procman;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.spine3.annotation.Subscribe;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Wrapper;
import org.spine3.server.command.Assign;
import org.spine3.server.commandbus.CommandBus;
import org.spine3.server.commandbus.CommandDispatcher;
import org.spine3.server.commandstore.CommandStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.server.tenant.TenantAwareTest;
import org.spine3.server.tenant.TenantIndex;
import org.spine3.test.Given;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.procman.ProjectId;
import org.spine3.test.procman.command.AddTask;
import org.spine3.test.procman.command.CreateProject;
import org.spine3.test.procman.command.StartProject;
import org.spine3.test.procman.event.ProjectCreated;
import org.spine3.test.procman.event.ProjectStarted;
import org.spine3.test.procman.event.TaskAdded;
import org.spine3.testdata.Sample;
import org.spine3.type.CommandClass;
import org.spine3.type.EventClass;
import org.spine3.validate.AnyValidatingBuilder;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.procman.ProcManTransaction.start;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

@SuppressWarnings("OverlyCoupledClass")
public class ProcessManagerShould {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);
    private static final EventContext EVENT_CONTEXT = EventContext.getDefaultInstance();

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private CommandBus commandBus;

    private TestProcessManager processManager;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = StorageFactorySwitch.get(true);
        final TenantIndex tenantIndex = TenantAwareTest.createTenantIndex(false, storageFactory);
        final CommandStore commandStore = spy(
                new CommandStore(storageFactory, tenantIndex)
        );

        commandBus = spy(CommandBus.newBuilder()
                                   .setCommandStore(commandStore)
                                   .build());
        processManager = Given.processManagerOfClass(TestProcessManager.class)
                              .withId(ID)
                              .withVersion(2)
                              .withState(Any.getDefaultInstance())
                              .build();
    }

    @Test
    public void have_default_state_initially() {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Test
    public void dispatch_event() {
        testDispatchEvent(Sample.messageOfType(ProjectStarted.class));
    }

    @Test
    public void dispatch_several_events() {
        testDispatchEvent(Sample.messageOfType(ProjectCreated.class));
        testDispatchEvent(Sample.messageOfType(TaskAdded.class));
        testDispatchEvent(Sample.messageOfType(ProjectStarted.class));
    }

    private void testDispatchEvent(Message event) {
        final ProcManTransaction<?, ?, ?> tx = start(processManager);
        processManager.dispatchEvent(event, EVENT_CONTEXT);
        tx.commit();
        assertEquals(AnyPacker.pack(event), processManager.getState());
    }

    @Test
    public void dispatch_command() {
        testDispatchCommand(addTask());
    }

    @Test
    public void dispatch_several_commands() {
        commandBus.register(new AddTaskDispatcher());
        processManager.setCommandBus(commandBus);

        testDispatchCommand(createProject());
        testDispatchCommand(addTask());
        testDispatchCommand(startProject());
    }

    private List<Event> testDispatchCommand(Message commandMsg) {
        final CommandEnvelope envelope = CommandEnvelope.of(requestFactory.command()
                                                                          .create(commandMsg));
        final ProcManTransaction<?, ?, ?> tx = start(processManager);
        final List<Event> events = processManager.dispatchCommand(envelope);
        tx.commit();
        assertEquals(AnyPacker.pack(commandMsg), processManager.getState());
        return events;
    }

    @Test
    public void dispatch_command_and_return_events() {
        final List<Event> events = testDispatchCommand(createProject());

        assertEquals(1, events.size());
        final Event event = events.get(0);
        assertNotNull(event);
        final ProjectCreated message = unpack(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    /**
     * Tests command routing.
     *
     * @see TestProcessManager#handle(StartProject, CommandContext)
     */
    @Test
    public void route_commands() {
        // Add dispatcher for the routed command. Otherwise the command would reject the command.
        commandBus.register(new AddTaskDispatcher());
        processManager.setCommandBus(commandBus);

        final List<Event> events = testDispatchCommand(startProject());

        // There's only one event generated.
        assertEquals(1, events.size());

        final Event event = events.get(0);

        // The producer of the event is our Process Manager.
        assertEquals(processManager.getId(), Events.getProducer(event.getContext()));

        final Message message = AnyPacker.unpack(event.getMessage());

        // The event type is CommandRouted.
        assertTrue(message instanceof CommandRouted);

        final CommandRouted commandRouted = (CommandRouted) message;

        // The source of the command is StartProject.
        assertTrue(getMessage(commandRouted.getSource()) instanceof StartProject);
        verifyPostedCmd(commandRouted.getProduced(0));
    }

    @SuppressWarnings("unchecked")
    private void verifyPostedCmd(Command cmd) {
        // The produced command was posted to CommandBus once, and the same
        // command is in the generated event.
        // We are not interested in observer instance here.
        verify(commandBus, times(1))
                .post(eq(cmd), any(StreamObserver.class));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_command() {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();

        final CommandEnvelope envelope = CommandEnvelope.of(
                requestFactory.command().create(unknownCommand)
        );
        processManager.dispatchCommand(envelope);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEvent = StringValue.getDefaultInstance();
        processManager.dispatchEvent(unknownEvent, EVENT_CONTEXT);
    }

    @Test
    public void return_handled_event_classes() {
        final Set<EventClass> classes =
                ProcessManager.TypeInfo.getEventClasses(TestProcessManager.class);
        assertEquals(3, classes.size());
        assertTrue(classes.contains(EventClass.of(ProjectCreated.class)));
        assertTrue(classes.contains(EventClass.of(TaskAdded.class)));
        assertTrue(classes.contains(EventClass.of(ProjectStarted.class)));
    }

    @Test
    public void create_iterating_router() {
        final StringValue commandMessage = Wrapper.forString("create_iterating_router");
        final CommandContext commandContext = requestFactory.createCommandContext();

        processManager.setCommandBus(mock(CommandBus.class));

        final IteratingCommandRouter router
                = processManager.newIteratingRouterFor(commandMessage,
                                                       commandContext);
        assertNotNull(router);

        assertEquals(commandMessage, getMessage(router.getSource()));
        assertEquals(commandContext, router.getSource()
                                           .getContext());
    }

    @Test(expected = IllegalStateException.class)
    public void require_command_bus_when_creating_router() {
        processManager.newRouterFor(StringValue.getDefaultInstance(),
                                    CommandContext.getDefaultInstance());
    }

    @Test
    public void create_router() {
        final StringValue commandMessage = Wrapper.forString("create_router");
        final CommandContext commandContext = requestFactory.createCommandContext();

        processManager.setCommandBus(mock(CommandBus.class));

        final CommandRouter router = processManager.newRouterFor(commandMessage, commandContext);
        assertNotNull(router);

        assertEquals(commandMessage, getMessage(router.getSource()));
        assertEquals(commandContext, router.getSource()
                                           .getContext());
    }

    private static CreateProject createProject() {
        return ((CreateProject.Builder) Sample.builderForType(CreateProject.class))
                .setProjectId(ID)
                .build();
    }

    private static StartProject startProject() {
        return ((StartProject.Builder) Sample.builderForType(StartProject.class))
                .setProjectId(ID)
                .build();
    }

    private static AddTask addTask() {
        return ((AddTask.Builder) Sample.builderForType(AddTask.class))
                .setProjectId(ID)
                .build();
    }

    @SuppressWarnings("UnusedParameters") // OK for test class.
    private static class TestProcessManager extends ProcessManager<ProjectId,
                                                                   Any,
                                                                   AnyValidatingBuilder> {

        private TestProcessManager(ProjectId id) {
            super(id);
        }

        @Subscribe
        public void on(ProjectCreated event, EventContext ignored) {
            getBuilder().mergeFrom(AnyPacker.pack(event));
        }

        @Subscribe
        public void on(TaskAdded event, EventContext ignored) {
            getBuilder().mergeFrom(AnyPacker.pack(event));
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext ignored) {
            getBuilder().mergeFrom(AnyPacker.pack(event));
        }

        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ignored) {
            getBuilder().mergeFrom(AnyPacker.pack(command));
            return ((ProjectCreated.Builder) Sample.builderForType(ProjectCreated.class))
                    .setProjectId(command.getProjectId())
                    .build();
        }

        @Assign
        TaskAdded handle(AddTask command, CommandContext ignored) {
            getBuilder().mergeFrom(AnyPacker.pack(command));
            return ((TaskAdded.Builder) Sample.builderForType(TaskAdded.class))
                    .setProjectId(command.getProjectId())
                    .build();
        }

        @Assign
        CommandRouted handle(StartProject command, CommandContext context) {
            getBuilder().mergeFrom(AnyPacker.pack(command));

            final Message addTask = ((AddTask.Builder) Sample.builderForType(AddTask.class))
                    .setProjectId(command.getProjectId())
                    .build();
            final CommandRouted route = newRouterFor(command, context)
                    .add(addTask)
                    .routeAll();
            return route;
        }
    }

    @Test
    public void have_TypeInfo_utility_class() {
        assertHasPrivateParameterlessCtor(ProcessManager.TypeInfo.class);
    }

    private static class AddTaskDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
            // Do nothing in this dummy dispatcher.
        }
    }
}
