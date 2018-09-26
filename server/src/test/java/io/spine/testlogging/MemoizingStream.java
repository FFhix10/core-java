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

package io.spine.testlogging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Dmytro Dashenkov
 */
final class MemoizingStream extends OutputStream {

    private static final int ONE_MEGABYTE = 1024 * 1024;

    private final ByteBuffer memory = ByteBuffer.allocate(ONE_MEGABYTE);

    @Override
    public void write(int b) {
        if (b >= 0) {
            @SuppressWarnings("NumericCastThatLosesPrecision")
                // Adheres to the OutputStream contract.
            byte byteValue = (byte) b;
            memory.put(byteValue);
        }
    }

    void clear() {
        memory.clear();
    }

    void flushTo(OutputStream stream) throws IOException {
        int entryCount = memory.position();
        for (int i = 0; i < entryCount; i++) {
            byte byteValue = memory.get(i);
            stream.write(byteValue);
        }
    }
}
