package com.technicalchallenge.repository;

import com.technicalchallenge.model.AdditionalInfo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdditionalInfoRepository extends JpaRepository<AdditionalInfo, Long> {

        /**
         * I use this to fetch all active AdditionalInfo records for a given entity.
         * This is typically used when loading all additional fields for a trade.
         */
        @Query("""
                        SELECT info FROM AdditionalInfo info
                        WHERE info.entityType = :entityType
                          AND info.entityId = :entityId
                          AND info.active = true
                        """)
        List<AdditionalInfo> findActiveByEntityTypeAndEntityId(
                        @Param("entityType") String entityType,
                        @Param("entityId") Long entityId);

        /**
         * I use this to fetch a single active AdditionalInfo record for a given entity
         * and field name. This is mainly used for settlement instructions where only
         * one active record should exist.
         */
        @Query("""
                        SELECT info FROM AdditionalInfo info
                        WHERE info.entityType = :entityType
                          AND info.entityId = :entityId
                          AND info.fieldName = :fieldName
                          AND info.active = true
                        """)
        AdditionalInfo findActiveByEntityTypeAndEntityIdAndFieldName(
                        @Param("entityType") String entityType,
                        @Param("entityId") Long entityId,
                        @Param("fieldName") String fieldName);

        /**
         * I use this derived query as an alternative way to fetch all active records
         * for a given entity without writing explicit JPQL.
         */
        List<AdditionalInfo> findByEntityTypeAndEntityIdAndActiveTrue(
                        String entityType,
                        Long entityId);

        /**
         * I use this method for general keyword searches across AdditionalInfo values.
         * Filtering happens in the database and pagination is enforced to avoid
         * unbounded result sets.
         */
        @Query("""
                        SELECT a FROM AdditionalInfo a
                        WHERE a.active = true
                          AND LOWER(a.fieldValue) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        """)
        Page<AdditionalInfo> searchByFieldValueContainingIgnoreCase(
                        @Param("keyword") String keyword,
                        Pageable pageable);

        /**
         * I use this method when I need to fetch a single active record and want
         * Optional semantics instead of returning null.
         */
        @Query("""
                        SELECT a FROM AdditionalInfo a
                        WHERE a.entityType = :entityType
                          AND a.entityId   = :entityId
                          AND a.fieldName  = :fieldName
                          AND a.active     = true
                        """)
        Optional<AdditionalInfo> findActiveOne(
                        @Param("entityType") String entityType,
                        @Param("entityId") Long entityId,
                        @Param("fieldName") String fieldName);

        /**
         * I use this method to support operations searching for trades by settlement
         * instruction keywords. The search is case insensitive and limited to active
         * settlement instruction records only.
         */
        @Query("""
                        SELECT a FROM AdditionalInfo a
                        WHERE a.entityType = 'TRADE'
                          AND a.fieldName  = 'SETTLEMENT_INSTRUCTIONS'
                          AND a.active     = true
                          AND LOWER(a.fieldValue) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        """)
        List<AdditionalInfo> searchTradeSettlementByKeyword(
                        @Param("keyword") String keyword);

        /**
         * I use this method to fetch settlement instructions for many trades in a
         * single query. This avoids N+1 query issues when enriching trade lists
         * with settlement data.
         */
        @Query("""
                        SELECT a FROM AdditionalInfo a
                        WHERE a.entityType = :entityType
                          AND a.entityId IN :entityIds
                          AND a.fieldName = :fieldName
                          AND a.active = true
                        """)
        List<AdditionalInfo> findByEntityTypeAndEntityIdInAndFieldName(
                        @Param("entityType") String entityType,
                        @Param("entityIds") List<Long> entityIds,
                        @Param("fieldName") String fieldName);

}
