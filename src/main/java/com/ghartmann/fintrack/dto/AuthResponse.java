package com.ghartmann.fintrack.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {
}