package com.tranche.allocation.repository;

import com.tranche.allocation.domain.InvestmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestmentOrderRepository extends JpaRepository<InvestmentOrder, Long> {

    Optional<InvestmentOrder> findByIdempotencyKeyAndInvestor_Id(String idempotencyKey, Long investorId);
}
