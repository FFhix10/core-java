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

package io.spine.server.event.enrich;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.server.event.given.EventMessageEnricherTestEnv.Enrichment;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectCreatedEnrichment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("MessageEnrichment should")
class EventEnrichmentTest {

    private EventEnrichment<ProjectCreated, ProjectCreatedEnrichment> enrichment;

    @BeforeEach
    void setUp() {
        Enricher enricher = Enrichment.newEventEnricher();
        this.enrichment = EventEnrichment.create(
                enricher,
                ProjectCreated.class,
                ProjectCreatedEnrichment.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Message.class, Empty.getDefaultInstance())
                .setDefault(EventContext.class, EventContext.getDefaultInstance())
                .testAllPublicInstanceMethods(enrichment);
    }

    @Test
    @DisplayName("be inactive when created")
    void beInactiveWhenCreated() {
        assertFalse(enrichment.isActive());
    }
}