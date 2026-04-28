package com.taskmanager.api.dto.auth;

import com.taskmanager.domain.user.Role;

import java.util.Set;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String name,
        Set<Role> roles
) {}
