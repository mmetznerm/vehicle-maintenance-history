package com.mmetzner.vmh.auth.application.dto;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken
) {
}