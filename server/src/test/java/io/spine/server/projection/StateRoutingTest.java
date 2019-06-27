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

package io.spine.server.projection;

import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.route.given.sur.ArtistMood;
import io.spine.server.route.given.sur.ArtistMoodRepo;
import io.spine.server.route.given.sur.ArtistName;
import io.spine.server.route.given.sur.Gallery;
import io.spine.server.route.given.sur.MagazineAggregate;
import io.spine.server.route.given.sur.Manifesto;
import io.spine.server.route.given.sur.Surrealism;
import io.spine.server.route.given.sur.Works;
import io.spine.server.route.given.sur.WorksProjection;
import io.spine.server.route.given.sur.command.PublishArticle;
import io.spine.server.route.given.sur.event.PieceOfArtCreated;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.route.given.sur.Surrealism.BRETON;
import static io.spine.server.route.given.sur.Surrealism.GOLL;

@DisplayName("ProjectionRepository state routing should")
class StateRoutingTest {

    private BlackBoxBoundedContext<?> context;

    @BeforeEach
    void setupContext() {
        context = BlackBoxBoundedContext.from(
                BoundedContextBuilder
                        .assumingTests()
                        .add(DefaultRepository.of(MagazineAggregate.class)));
    }

    @Test
    @DisplayName("support explicit routing")
    void explicit() {
        context.with(new ArtistMoodRepo());

        context.receivesCommand(
                PublishArticle.newBuilder()
                              .setMagazineName("Manifeste du surréalisme")
                              .setArticle(AnyPacker.pack(
                                      Manifesto.newBuilder()
                                               .setAuthor(GOLL)
                                               .setText("Lorem ipsum")
                                               .build()))
                              .build()
        );

        context.assertEntityWithState(ArtistMood.class, BRETON)
               .hasStateThat()
               .comparingExpectedFieldsOnly()
               .isEqualTo(ArtistMood.newBuilder()
                                    .setMood(ArtistMood.Mood.ANGER)
                                    .build());
    }

    @Test
    @DisplayName("route state by the first field matching the ID type")
    void implicit() {
        context.with(new Gallery(),
                     DefaultRepository.of(WorksProjection.class));

        ArtistName artist = Surrealism.name("André Masson");
        Any automaticDrawing = AnyPacker.pack(StringValue.of("Automatic Drawing"));
        context.receivesEvent(
                PieceOfArtCreated
                        .newBuilder()
                        .setUuid(Identifier.newUuid())
                        .setArtist(artist)
                        .setContent(automaticDrawing)
                        .build()
        );

        ProtoSubject<?, Message> assertWorks =
                context.assertEntity(WorksProjection.class, artist)
                       .hasStateThat();
        Works expectedWork =
                Works.newBuilder()
                     .addWork(automaticDrawing)
                     .build();
        assertWorks.comparingExpectedFieldsOnly()
                   .isEqualTo(expectedWork);
    }
}