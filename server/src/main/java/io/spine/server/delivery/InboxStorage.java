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

package io.spine.server.delivery;

import io.spine.annotation.SPI;
import io.spine.server.storage.Storage;
import io.spine.validate.Validated;

import java.util.List;

import static com.google.common.collect.Streams.stream;
import static io.spine.server.delivery.InboxMessageStatus.DELIVERED;
import static java.util.stream.Collectors.toList;

/**
 * A contract for storages of {@link Inbox} messages.
 *
 * <p>The records a storage of this type are spreads across shards identified by a
 * {@linkplain ShardIndex shard index}.
 *
 * <p>Typically, the storage instance is specific to the
 * {@linkplain io.spine.server.ServerEnvironment server environment} and is used across
 * {@code BoundedContext}s to store the delivered messages.
 */
@SPI
public interface InboxStorage
        extends Storage<InboxMessageId, InboxMessage, InboxReadRequest> {

    /**
     * Reads the contents of the storage by the given shard index and returns the first page
     * of the results.
     *
     * <p>The older items go first.
     *
     * @param index
     *         the shard index to return the results for
     * @param pageSize
     *         the maximum number of the elements per page
     * @return the first page of the results
     */
    Page<InboxMessage> readAll(ShardIndex index, int pageSize);

    /**
     * Writes a message to the storage.
     *
     * @param message
     *         a message to write
     */
    void write(InboxMessage message);

    /**
     * Writes several messages to the storage.
     *
     * @param messages
     *         messages to write
     */
    void writeAll(Iterable<InboxMessage> messages);

    /**
     * Removes the passed messages from the storage.
     *
     * <p>Does nothing for messages that aren't in the storage already.
     *
     * @param messages
     *         the messages to remove
     */
    void removeAll(Iterable<InboxMessage> messages);

    /**
     * Marks the messages as {@linkplain InboxMessageStatus#DELIVERED delivered} and writes
     * the updated messages.
     *
     * @param messages
     *         the messages to mark as delivered
     * @implNote The messages aren't additionally validated after marking as delivered to
     *         improve the performance.
     */
    default void markDelivered(Iterable<InboxMessage> messages) {
        List<@Validated InboxMessage> updated =
                stream(messages).map((m) -> m.toBuilder()
                                             .setStatus(DELIVERED)
                                             .build())
                                .collect(toList());
        writeAll(updated);
    }
}