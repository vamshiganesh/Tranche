package com.tranche.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new com.tranche.common.exception.UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public static UUID requireCurrentPublicId() {
        return requireCurrentUser().getPublicId();
    }
}
