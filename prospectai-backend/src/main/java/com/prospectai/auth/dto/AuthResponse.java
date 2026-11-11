package com.prospectai.auth.dto;

import com.prospectai.user.Role;

public record AuthResponse(
    String token,
    String id,
    String name,
    String email,
    Role role
) {}
