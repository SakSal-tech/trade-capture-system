package com.technicalchallenge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.technicalchallenge.model.AdditionalInfoAudit;

@Repository
public interface AdditionalInfoAuditRepository extends JpaRepository<AdditionalInfoAudit, Long> {

    // Find all audit records for a given trade (sorted by newest first)
    List<AdditionalInfoAudit> findByTradeIdOrderByChangedAtDesc(Long tradeId);
}
