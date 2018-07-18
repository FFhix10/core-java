/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.commandbus;

@SuppressWarnings({"DuplicateStringLiteralInspection" /* Common test display names. */,
        "unused" /* JUnit nested classes considered unused in abstract test class */})
abstract class CommandStoreTest extends AbstractCommandBusTestSuite {

    CommandStoreTest(boolean multitenant) {
        super(multitenant);
    }

//    @Nested
//    @DisplayName("set command status to")
//    class SetCommandStatusTo {
//
//        @Test
//        @DisplayName("`OK` when handler returns")
//        void ok() {
//            commandBus.register(createProjectHandler);
//
//            final Command command = requestFactory.command()
//                                                  .create(createProjectMessage());
//            commandBus.post(command, observer);
//
//            final TenantId tenantId = command.getContext()
//                                             .getActorContext()
//                                             .getTenantId();
//            final CommandId commandId = command.getId();
//            final ProcessingStatus status = getStatus(commandId, tenantId);
//
//            assertEquals(CommandStatus.OK, status.getCode());
//        }
//
//        @Test
//        @DisplayName("`ERROR` when dispatcher throws exception")
//        void errorForDispatcherException() {
//            final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
//            commandBus.register(dispatcher);
//            final Command command = requestFactory.command()
//                                                  .create(createProjectMessage());
//
//            commandBus.post(command, observer);
//
//            // Check that the logging was called.
//            final CommandEnvelope envelope = CommandEnvelope.of(command);
//            verify(log).errorHandling(dispatcher.exception(),
//                                      envelope.getMessage(),
//                                      envelope.getId());
//
//            // Check that the command status has the correct code,
//            // and the error matches the thrown exception.
//            assertHasErrorStatusWithMessage(envelope, dispatcher.exception().getMessage());
//        }
//
//        @Test
//        @DisplayName("`ERROR` when handler throws exception")
//        void errorForHandlerException() {
//            ModelTests.clearModel();
//
//            final RuntimeException exception = new IllegalStateException("handler throws");
//            final CommandStoreTestAssets assets =
//                    new CommandStoreTestAssets(eventBus, commandBus, requestFactory);
//            final Command command = givenThrowingHandler(exception, assets);
//            final CommandEnvelope envelope = CommandEnvelope.of(command);
//
//            commandBus.post(command, observer);
//
//            // Check that the logging was called.
//            verify(log).errorHandling(eq(exception),
//                                      eq(envelope.getMessage()),
//                                      eq(envelope.getId()));
//
//            final String errorMessage = exception.getMessage();
//            assertHasErrorStatusWithMessage(envelope, errorMessage);
//        }
//
//        @Test
//        @DisplayName("`REJECTED` when handler throws rejection")
//        void rejectionForHandlerRejection() {
//            ModelTests.clearModel();
//
//            final TestRejection rejection = new TestRejection();
//            final CommandStoreTestAssets assets =
//                    new CommandStoreTestAssets(eventBus, commandBus, requestFactory);
//            final Command command = givenRejectingHandler(rejection, assets);
//            final CommandId commandId = command.getId();
//            final Message commandMessage = getMessage(command);
//
//            commandBus.post(command, observer);
//
//            // Check that the logging was called.
//            verify(log).rejectedWith(eq(rejection), eq(commandMessage), eq(commandId));
//
//            // Check that the status has the correct code,
//            // and the rejection matches the thrown rejection.
//            final TenantId tenantId = command.getContext()
//                                             .getActorContext()
//                                             .getTenantId();
//            final ProcessingStatus status = getStatus(commandId, tenantId);
//
//            assertEquals(CommandStatus.REJECTED, status.getCode());
//            assertEquals(toRejection(rejection, command).getMessage(),
//                         status.getRejection()
//                               .getMessage());
//        }
//
//        @Test
//        @DisplayName("`ERROR` for expired scheduled command")
//        void errorForExpiredCommand() {
//            final List<Command> commands = newArrayList(createProject(),
//                                                        addTask(),
//                                                        startProject());
//            final Duration delay = fromMinutes(5);
//            final Timestamp schedulingTime = TimeTests.Past.minutesAgo(10); // time to post passed
//            storeAsScheduled(commands, delay, schedulingTime);
//
//            commandBus.rescheduleCommands();
//
//            for (Command cmd : commands) {
//                final CommandEnvelope envelope = CommandEnvelope.of(cmd);
//                final Message msg = envelope.getMessage();
//                final CommandId id = envelope.getId();
//
//                // Check the expired status error was set.
//                final ProcessingStatus status = getProcessingStatus(envelope);
//
//                // Check that the logging was called.
//                verify(log).errorExpiredCommand(msg, id);
//
//                final Error expected = CommandExpiredException.commandExpired(cmd);
//                assertEquals(expected, status.getError());
//            }
//        }
//
//        /**
//         * Checks that the command status has the correct code, and the stored error message
//         * matches the passed message.
//         *
//         * <p>The check is performed as a tenant-aware function using the tenant ID from the
//         * command.
//         */
//        private void assertHasErrorStatusWithMessage(CommandEnvelope commandEnvelope,
//                                                     String errorMessage) {
//            final ProcessingStatus status = getProcessingStatus(commandEnvelope);
//            assertEquals(CommandStatus.ERROR, status.getCode());
//            assertEquals(errorMessage, status.getError()
//                                             .getMessage());
//        }
//
//        private ProcessingStatus getProcessingStatus(CommandEnvelope commandEnvelope) {
//            final TenantId tenantId = commandEnvelope.getCommandContext()
//                                                     .getActorContext()
//                                                     .getTenantId();
//            final TenantAwareFunction<CommandId, ProcessingStatus> func =
//                    new TenantAwareFunction<CommandId, ProcessingStatus>(tenantId) {
//                        @Override
//                        public ProcessingStatus apply(@Nullable CommandId input) {
//                            return commandStore.getStatus(checkNotNull(input));
//                        }
//                    };
//            final ProcessingStatus result = func.execute(commandEnvelope.getId());
//            return result;
//        }
//    }
//
//    @Nested
//    @DisplayName("store")
//    class Store {
//
//        @Test
//        @DisplayName("command")
//        void command() {
//            final Command command = requestFactory.command()
//                                                  .create(createProjectMessage());
//            commandStore.store(command);
//
//            final TenantId tenantId = command.getContext()
//                                             .getActorContext()
//                                             .getTenantId();
//            final CommandId commandId = command.getId();
//            final ProcessingStatus status = getStatus(commandId, tenantId);
//
//            assertEquals(CommandStatus.RECEIVED, status.getCode());
//        }
//
//        @Test
//        @DisplayName("command with status")
//        void commandWithStatus() {
//            final Command command = requestFactory.command()
//                                                  .create(createProjectMessage());
//            final CommandStatus commandStatus = CommandStatus.OK;
//            commandStore.store(command, commandStatus);
//
//            final TenantId tenantId = command.getContext()
//                                             .getActorContext()
//                                             .getTenantId();
//            final CommandId commandId = command.getId();
//            final ProcessingStatus status = getStatus(commandId, tenantId);
//
//            assertEquals(commandStatus, status.getCode());
//        }
//
//        @Test
//        @DisplayName("command with error")
//        void commandWithError() {
//            final Command command = requestFactory.command()
//                                                  .create(createProjectMessage());
//            @SuppressWarnings("ThrowableNotThrown") // Creation without throwing is needed for test.
//            final DuplicateCommandException exception = of(command);
//            commandStore.storeWithError(command, exception);
//
//            final TenantId tenantId = command.getContext()
//                                             .getActorContext()
//                                             .getTenantId();
//            final CommandId commandId = command.getId();
//            final ProcessingStatus status = getStatus(commandId, tenantId);
//
//            assertEquals(CommandStatus.ERROR, status.getCode());
//            assertEquals(exception.asError(), status.getError());
//        }
//
//        @Test
//        @DisplayName("command with exception")
//        void commandWithException() {
//            final Command command = requestFactory.command()
//                                                  .create(createProjectMessage());
//            @SuppressWarnings("ThrowableNotThrown") // Creation without throwing is needed for test.
//            final DuplicateCommandException exception = of(command);
//            commandStore.store(command, exception);
//
//            final TenantId tenantId = command.getContext()
//                                             .getActorContext()
//                                             .getTenantId();
//            final CommandId commandId = command.getId();
//            final ProcessingStatus status = getStatus(commandId, tenantId);
//
//            assertEquals(CommandStatus.ERROR, status.getCode());
//            assertEquals(fromThrowable(exception), status.getError());
//        }
//    }
//
//    private ProcessingStatus getStatus(CommandId commandId, final TenantId tenantId) {
//        final ProcessingStatus status =
//                CommandStoreTestEnv.getStatus(commandId, tenantId, commandStore);
//        return status;
//    }
}
