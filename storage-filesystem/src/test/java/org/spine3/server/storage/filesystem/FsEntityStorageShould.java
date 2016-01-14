/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.filesystem;

import org.junit.After;
import org.junit.Ignore;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageShould;
import org.spine3.server.storage.StorageFactory;

import java.io.IOException;

/**
 * File system implementation of {@link EntityStorage} tests.
 *
 * @author Alexander Litus
 */
@Ignore
public class FsEntityStorageShould extends EntityStorageShould {

    private static final StorageFactory FACTORY = FileSystemStorageFactory.newInstance(FsEntityStorageShould.class);

    @SuppressWarnings("ConstantConditions")
    private static final EntityStorage<String> STORAGE = FACTORY.createEntityStorage(null);

    public FsEntityStorageShould() {
        super(STORAGE);
    }

    @After
    public void tearDownTest() throws IOException {
        FACTORY.close();
    }
}
