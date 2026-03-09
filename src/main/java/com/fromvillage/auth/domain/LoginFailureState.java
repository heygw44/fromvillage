package com.fromvillage.auth.domain;

import java.time.Instant;

public record LoginFailureState(
        int failedCount,
        Instant lockedUntil
) {

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
