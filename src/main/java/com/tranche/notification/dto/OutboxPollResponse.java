package com.tranche.notification.dto;

public record OutboxPollResponse(int processed, int published) {
}
