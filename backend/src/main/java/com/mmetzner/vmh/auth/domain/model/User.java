package com.mmetzner.vmh.auth.domain.model;

import java.util.UUID;

public record User(
        UUID id,
        String fullName,
        String emailOrPhone,
        String passwordHash
) {

}