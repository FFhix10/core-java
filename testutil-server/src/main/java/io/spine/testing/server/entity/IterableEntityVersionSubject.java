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

package io.spine.testing.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.IterableOfProtosSubject;
import io.spine.core.Version;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertAbout;
import static io.spine.testing.server.entity.EntityVersionSubject.entityVersion;

@VisibleForTesting
public final class IterableEntityVersionSubject
        extends IterableOfProtosSubject<IterableEntityVersionSubject, Version, Iterable<Version>> {

    private IterableEntityVersionSubject(FailureMetadata failureMetadata,
                                         @Nullable Iterable<Version> versions) {
        super(failureMetadata, versions);
    }

    public static IterableEntityVersionSubject
    assertEntityVersions(@Nullable Iterable<Version> versions) {
        return assertAbout(entityVersions()).that(versions);
    }

    public void containsAllNewerThan(Version version) {
        checkNotNull(version);
        assertExists();
        actual().forEach(v -> assertVersion(v).isNewerThan(version));
    }

    public void containsAllNewerOrEqualTo(Version version) {
        checkNotNull(version);
        assertExists();
        actual().forEach(v -> assertVersion(v).isNewerOrEqualTo(version));
    }

    public void containsAllOlderThan(Version version) {
        checkNotNull(version);
        assertExists();
        actual().forEach(v -> assertVersion(v).isOlderThan(version));
    }

    public void containsAllOlderOrEqualTo(Version version) {
        checkNotNull(version);
        assertExists();
        actual().forEach(v -> assertVersion(v).isOlderOrEqualTo(version));
    }

    public EntityVersionSubject containsSingleEntityVersionThat() {
        assertContainsSingleItem();
        Version version = actual().iterator()
                                  .next();
        return assertVersion(version);
    }

    private EntityVersionSubject assertVersion(Version version) {
        return check("singleEntityVersion()").about(entityVersion())
                                             .that(version);
    }

    private void assertContainsSingleItem() {
        assertExists();
        hasSize(1);
    }

    private void assertExists() {
        isNotNull();
    }

    public static
    Subject.Factory<IterableEntityVersionSubject, Iterable<Version>> entityVersions() {
        return IterableEntityVersionSubject::new;
    }
}
