package com.axel.alvarado.coworking_service.dto;

public record AuthResponse(
        String token,
        String tokenType) {
    public AuthResponse(String token) {
        this(token, "Bearer");
    }
}
