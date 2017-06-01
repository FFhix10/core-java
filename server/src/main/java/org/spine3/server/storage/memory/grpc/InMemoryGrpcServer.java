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

package org.spine3.server.storage.memory.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.spine3.annotation.Internal;
import org.spine3.server.BoundedContext;

import java.io.IOException;

import static org.spine3.util.Exceptions.illegalStateWithCauseOf;

/**
 * Starts a gRPC server serving access to in-memory data storage.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class InMemoryGrpcServer {

    private static final String SERVER_NAME = "InMemory";

    private Server grpcServer;

    public InMemoryGrpcServer(BoundedContext boundedContext) {
        this.grpcServer = InProcessServerBuilder
                .forName(SERVER_NAME)
                .addService(new ProjectionStorageService(boundedContext))
                .addService(new EventStreamService(boundedContext))
                .build();
    }

    public static InMemoryGrpcServer startOn(BoundedContext boundedContext) {
        InMemoryGrpcServer grpcServer = new InMemoryGrpcServer(boundedContext);
        try {
            grpcServer.start();
        } catch (IOException e) {
            throw illegalStateWithCauseOf(e);
        }
        return grpcServer;
    }

    /**
     * Creates a default channel for read/write operations to in-memory storages via
     * {@link ProjectionStorageService
     * ProjectionStorageService}.
     */
    public static ManagedChannel createDefaultChannel() {
        final ManagedChannel result = InProcessChannelBuilder.forName(SERVER_NAME)
                                                             .build();
        return result;
    }

    public void start() throws IOException {
        grpcServer.start();
    }

    public void shutdown() {
        grpcServer.shutdownNow();
        grpcServer = null;
    }
}
