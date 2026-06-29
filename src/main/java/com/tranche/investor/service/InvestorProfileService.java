package com.tranche.investor.service;

import com.tranche.auth.domain.User;
import com.tranche.common.config.InvestorProperties;
import com.tranche.common.exception.ConflictException;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.repository.InvestorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class InvestorProfileService {

    private final InvestorProfileRepository investorProfileRepository;
    private final InvestorProperties investorProperties;

    public InvestorProfileService(
            InvestorProfileRepository investorProfileRepository,
            InvestorProperties investorProperties
    ) {
        this.investorProfileRepository = investorProfileRepository;
        this.investorProperties = investorProperties;
    }

    @Transactional
    public InvestorProfile createForUser(User user) {
        if (investorProfileRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Investor profile already exists");
        }

        InvestorProfile profile = new InvestorProfile();
        profile.setUser(user);
        profile.setWalletBalance(investorProperties.defaultWalletBalance());
        profile.setLockedBalance(BigDecimal.ZERO);
        profile.setCurrency(investorProperties.defaultCurrency());
        return investorProfileRepository.save(profile);
    }
}
