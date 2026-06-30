package com.tranche.investor.repository;

import com.tranche.common.domain.VerificationStatus;
import com.tranche.common.domain.VerificationStatus;
import com.tranche.investor.domain.InvestorProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestorProfileRepository extends JpaRepository<InvestorProfile, Long> {

    Optional<InvestorProfile> findByUser_Id(Long userId);

    Optional<InvestorProfile> findByUser_PublicId(UUID publicId);

    boolean existsByUser_Id(Long userId);

    /**
     * Locks the investor wallet row for fund reservation during allocation.
     * Always acquire after the opportunity lock to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InvestorProfile p WHERE p.user.id = :userId")
    Optional<InvestorProfile> findByUserIdForUpdate(@Param("userId") Long userId);

    List<InvestorProfile> findByKycStatus(VerificationStatus kycStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InvestorProfile p
            SET p.walletBalance = :balance,
                p.lockedBalance = 0,
                p.kycStatus = :approved,
                p.version = p.version + 1
            """)
    void resetAllWalletsForTests(
            @Param("balance") java.math.BigDecimal balance,
            @Param("approved") VerificationStatus approved
    );
}
