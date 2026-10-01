package com.carbuyer.user.api.dto;

public record CreateUserCommand(
        String name,
        String email,
        String rawPassword
) {
}
