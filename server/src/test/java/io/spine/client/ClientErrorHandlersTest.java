/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`Client` should pass error handlers to `ClientRequest` for")
public class ClientErrorHandlersTest extends AbstractClientTest {

    private final ErrorHandler errorHandler = (throwable) -> {};
    private final PostingErrorHandler postingErrorHandler = (message, error) -> {};

    private ClientRequest request;

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.users());
    }

    /**
     * Adds custom error handlers for the client instance to be used in this test suite.
     */
    @Override
    protected Client.Builder newClientBuilder(String serverName) {
        Client.Builder builder = super.newClientBuilder(serverName);
        builder.onStreamingError(errorHandler)
               .onPostingError(postingErrorHandler);
        return builder;
    }

    @BeforeEach
    void createRequest() {
        request = client().asGuest();
    }

    @Test
    @DisplayName("a streaming error handler")
    void streamingHandler() {
        assertThat(request.streamingErrorHandler())
                .isEqualTo(errorHandler);
    }

    @Test
    @DisplayName("a posting error handler")
    void postingHandler() {
        assertThat(request.postingErrorHandler())
                .isEqualTo(postingErrorHandler);
    }
}
