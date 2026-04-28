package com.taskmanager.security;

import com.taskmanager.api.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public AppUserDetails current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new ForbiddenException("No authenticated user");
        }
        return details;
    }

    public Long currentId() {
        return current().getUserId();
    }
}
