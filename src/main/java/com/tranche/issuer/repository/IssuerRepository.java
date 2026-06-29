package com.tranche.issuer.repository;

import com.tranche.issuer.domain.Issuer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IssuerRepository extends JpaRepository<Issuer, Long> {

    Optional<Issuer> findByUser_Id(Long userId);

    Optional<Issuer> findByUser_PublicId(UUID publicId);

    boolean existsByUser_Id(Long userId);
}
