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

package org.spine3.server.command;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandEnvelope;
import org.spine3.base.CommandId;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Response;
import org.spine3.client.CommandFactory;
import org.spine3.protobuf.Durations2;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.TestCommandFactory;
import org.spine3.protobuf.Durations;
import org.spine3.server.type.CommandClass;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.StartProject;
import org.spine3.test.command.event.ProjectCreated;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.base.Commands.setSchedule;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.server.command.error.CommandExpiredException.commandExpiredError;
import static org.spine3.test.TimeTests.Past.minutesAgo;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps.minutesAgo;
import static org.spine3.server.command.CommandScheduler.setSchedule;
import static org.spine3.server.command.Given.Command.createProject;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class CommandBusShouldHandleCommandStatus extends AbstractCommandBusTestSuite {

    public CommandBusShouldHandleCommandStatus() {
        super(true);
    }

    //TODO:2017-02-14:alexander.yevsyukov: Enable back when obtaining command status is available
    @Ignore
    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(createProjectHandler);

        final Command command = createProject();
        commandBus.post(command, responseObserver);

        // See that we called CommandStore only once with the right command ID.
//        verify(commandStore).setCommandStatusOk(command);
    }

    //TODO:2017-02-14:alexander.yevsyukov: Enable back when obtaining command status is available
    @Ignore
    @Test
    public void set_command_status_to_error_when_dispatcher_throws() throws Exception {
        final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
        commandBus.register(dispatcher);
        final Command command = commandFactory.createCommand(Given.CommandMessage.createProjectMessage());

        commandBus.post(command, responseObserver);

//        verify(commandStore, atMost(1)).updateStatus(command, dispatcher.exception);

        final CommandEnvelope envelope = new CommandEnvelope(command);
        verify(log).errorHandling(dispatcher.exception, envelope.getCommandMessage(), envelope.getCommandId());
    }

    //TODO:2017-02-14:alexander.yevsyukov: Enable back when obtaining command status is available
    @Ignore
    @Test
    public void set_command_status_to_failure_when_handler_throws_failure() throws TestFailure, TestThrowable {
        final TestFailure failure = new TestFailure();
        final Command command = givenThrowingHandler(failure);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

//        verify(commandStore, atMost(1)).updateStatus(eq(command), eq(failure.toMessage()));
        verify(log).failureHandling(eq(failure), eq(commandMessage), eq(commandId));
    }

    //TODO:2017-02-14:alexander.yevsyukov: Enable back when obtaining command status is available
    @Ignore
    @Test
    public void set_command_status_to_failure_when_handler_throws_exception() throws TestFailure, TestThrowable {
        final RuntimeException exception = new IllegalStateException("handler throws");
        final Command command = givenThrowingHandler(exception);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

//        verify(commandStore, atMost(1)).updateStatus(eq(command), eq(exception));
        verify(log).errorHandling(eq(exception), eq(commandMessage), eq(commandId));
    }

    //TODO:2017-02-14:alexander.yevsyukov: Enable back when obtaining command status is available
    @Ignore
    @Test
    public void set_command_status_to_failure_when_handler_throws_unknown_Throwable()
            throws TestFailure, TestThrowable {
        final Throwable throwable = new TestThrowable();
        final Command command = givenThrowingHandler(throwable);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

//        verify(commandStore, atMost(1))
//                .updateStatus(eq(command), eq(Errors.fromThrowable(throwable)));

        verify(log).errorHandlingUnknown(eq(throwable), eq(commandMessage), eq(commandId));
    }

    //TODO:2017-02-14:alexander.yevsyukov: Enable back when obtaining command status is available
    @Ignore
    @Test
    public void set_expired_scheduled_command_status_to_error_if_time_to_post_them_passed() {
        final List<Command> commands = newArrayList(Given.Command.createProject(),
                                                    Given.Command.addTask(),
                                                    Given.Command.startProject());
        final Duration delay = Durations2.fromMinutes(5);
        final Timestamp schedulingTime = minutesAgo(10); // time to post passed
        storeAsScheduled(commands, delay, schedulingTime);

        commandBus.rescheduler()
                  .doRescheduleCommands();

        for (Command cmd : commands) {
            final Message msg = getMessage(cmd);
            final CommandId id = getId(cmd);
//            verify(commandStore, atMost(1)).updateStatus(cmd, commandExpiredError(msg));
            verify(log).errorExpiredCommand(msg, id);
        }
    }

    private void storeAsScheduled(Iterable<Command> commands, Duration delay, Timestamp schedulingTime) {
        for (Command cmd : commands) {
            final Command cmdWithSchedule = setSchedule(cmd, delay, schedulingTime);
            commandStore.store(cmdWithSchedule, SCHEDULED);
        }
    }

    /**
     * A stub handler that throws passed `Throwable` in the command handler method.
     *
     * @see #set_command_status_to_failure_when_handler_throws_failure
     * @see #set_command_status_to_failure_when_handler_throws_exception
     * @see #set_command_status_to_failure_when_handler_throws_unknown_Throwable
     */
    private class ThrowingCreateProjectHandler extends CommandHandler {

        @Nonnull
        private final Throwable throwable;

        protected ThrowingCreateProjectHandler(@Nonnull Throwable throwable) {
            super(eventBus);
            this.throwable = throwable;
        }

        @Assign
        @SuppressWarnings({"unused", "ProhibitedExceptionThrown"})
            // Throwing is the purpose of this method.
        ProjectCreated handle(CreateProject msg, CommandContext context) throws Throwable {
            throw throwable;
        }
    }

    private <E extends Throwable> Command givenThrowingHandler(E throwable) {
        final CommandHandler handler = new ThrowingCreateProjectHandler(throwable);
        commandBus.register(handler);
        final CreateProject msg = Given.CommandMessage.createProjectMessage();
        final Command command = commandFactory.createCommand(msg);
        return command;
    }

    /*
     * Throwables.
     ********************/

    private static class TestFailure extends FailureThrowable {
        private static final long serialVersionUID = 1L;

        private TestFailure() {
            super(newStringValue("some Command message"),
                  CommandContext.getDefaultInstance(),
                  newStringValue(TestFailure.class.getName()));
        }
    }

    @SuppressWarnings("serial")
    private static class TestThrowable extends Throwable {
    }

    private static class ThrowingDispatcher implements CommandDispatcher {

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        private final RuntimeException exception = new RuntimeException("Some dispatching exception.");

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
            throw exception;
        }
    }
}
