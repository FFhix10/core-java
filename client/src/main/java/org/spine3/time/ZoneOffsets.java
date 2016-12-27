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

package org.spine3.time;

import org.spine3.protobuf.Durations;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utilities for working with ZoneOffset objects.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 * @see ZoneOffset
 */
public class ZoneOffsets {

    private ZoneOffsets() {
    }

    public static final ZoneOffset UTC = ZoneOffset.newBuilder()
                                                   .setId("UTC")
                                                   .setAmountSeconds(0)
                                                   .build();

    /**
     * Obtains the ZoneOffset instance using an offset in hours.
     */
    public static ZoneOffset ofHours(int hours) {
        final int maxHoursBound = 18;
        checkArgument(Math.abs(hours) <= maxHoursBound, "offset size must be between -18 and 18 hours inclusive");
        @SuppressWarnings("NumericCastThatLosesPrecision") // It is safe, as we check bounds of the argument.
        final int seconds = (int) Durations.toSeconds(Durations.ofHours(hours));
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }

    /**
     * Obtains the ZoneOffset instance using an offset in hours and minutes.
     */
    @SuppressWarnings("NumericCastThatLosesPrecision") // It is safe, as we check bounds of the argument.
    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        final int maxHoursBound = 17;
        final int maxMinutesBound = 60;
        checkArgument(Math.abs(hours) <= maxHoursBound, "offset hour size must be between -17 and 17 hours inclusive");
        checkArgument(Math.abs(minutes) <= maxMinutesBound, "offset minute size must be between -60 and 60 minutes inclusive");
        final int secondsInHours = (int) Durations.toSeconds(Durations.ofHours(hours));
        final int secondsInMinutes = (int) Durations.toSeconds(Durations.ofMinutes(minutes));
        final int seconds = secondsInHours + secondsInMinutes;
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }

    //TODO:2016-01-20:alexander.yevsyukov: Add other offsets
}
