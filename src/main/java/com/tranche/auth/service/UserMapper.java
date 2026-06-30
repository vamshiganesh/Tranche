package com.tranche.auth.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.dto.CurrentUserResponse;
import com.tranche.auth.dto.UserResponse;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.issuer.domain.Issuer;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                user.getCreatedAt()
        );
    }

    public static CurrentUserResponse toCurrentUserResponse(
            User user,
            InvestorProfile investorProfile,
            Issuer issuer
    ) {
        return new CurrentUserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                user.isEmailVerified(),
                investorProfile != null ? investorProfile.getWalletBalance() : null,
                investorProfile != null ? investorProfile.getKycStatus() : null,
                issuer != null,
                issuer != null ? issuer.getVerificationStatus() : null
        );
    }
}
