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

package io.spine.system.server.given.entity;

import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.system.server.PersonCreated;
import io.spine.system.server.PersonDetails;
import io.spine.system.server.PersonDetailsVBuilder;
import io.spine.system.server.PersonExposed;
import io.spine.system.server.PersonHidden;
import io.spine.system.server.PersonId;
import io.spine.type.TypeUrl;

/**
 * Test projection which subscribes to events generated by {@link PersonAggregate}
 * and changes its lifecycle flags as the “linked” aggregate state changes.
 *
 * @author Dmytro Dashenkov
 */
public class PersonProjection
        extends Projection<PersonId, PersonDetails, PersonDetailsVBuilder> {

    public static final TypeUrl TYPE = TypeUrl.of(PersonDetails.class);

    protected PersonProjection(PersonId id) {
        super(id);
    }

    @Subscribe
    public void on(PersonCreated event) {
        getBuilder().setId(event.getId())
                    .setName(event.getName());
    }

    @Subscribe
    public void on(PersonHidden event) {
        setDeleted(true);
    }

    @Subscribe
    public void on(PersonExposed event) {
        setDeleted(false);
    }
}
