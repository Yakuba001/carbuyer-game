package com.carbuyer.user.api.event;

public record UserCreatedEvent(
        Long userId,
        Integer maxSlots
) {
}
