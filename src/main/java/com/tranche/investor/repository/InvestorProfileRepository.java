package com.tranche.investor.repository;

import com.tranche.investor.domain.InvestorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvestorProfileRepository extends JpaRepository<InvestorProfile, Long> {

    Optional<InvestorProfile> findByUserId(Long userId);

    Optional<InvestorProfile> findByUser_PublicId(UUID publicId);

    boolean existsByUserId(Long userId);
}
