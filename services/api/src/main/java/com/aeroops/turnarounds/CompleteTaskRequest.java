package com.aeroops.turnarounds;

import java.time.Instant;

public record CompleteTaskRequest(Instant actualAt) {
}
