package com.prospectai.user.dto;

import com.prospectai.user.Role;
import com.prospectai.user.User;

import java.time.Instant;

public record UserResponse(
    String id,
    String name,
    String email,
    Role role,
    Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
