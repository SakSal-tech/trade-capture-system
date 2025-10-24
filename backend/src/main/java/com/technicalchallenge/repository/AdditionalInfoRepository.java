package com.technicalchallenge.repository;

import com.technicalchallenge.model.AdditionalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalInfoRepository extends JpaRepository<AdditionalInfo, Long> {

        /**
         * Retrieves all active records for AdditionalInfospecific entity type and
         * entity ID.
         * 
         * @param entityType The type of entity the information belongs to, e.g.
         *                   "TRADE".
         * @param entityId   The unique identifier of that entity, e.g. the Trade ID.
         * @return AdditionalInfolist of all active records for the given entity.
         *         This query filters out any inactive (soft-deleted) records by
         *         checking active = true}.
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
         * Retrieves AdditionalInfosingle active AdditionalInfo record for
         * AdditionalInfogiven entity type,
         * entity ID, and field name.
         * 
         * @param entityType The type of entity, e.g. "TRADE".
         * @param entityId   The ID of the entity record, e.g. the trade ID.
         * @param fieldName  The name of the field, e.g. "SETTLEMENT_INSTRUCTIONS".
         * @return The matching active AdditionalInfo ecord, or nul if none is found.
         *         This is useful when fetching AdditionalInfospecific piece of
         *         information such as
         *         settlement instructions for AdditionalInfogiven trade.
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
         * Derived query method to find all active AdditionalInfo records
         * for AdditionalInfogiven entity type and ID.
         *
         * @param entityType The type of entity, e.g. "TRADE".
         * @param entityId   The ID of the entity record.
         * @return AdditionalInfolist of all active AdditionalInfo records.
         *
         *         This method achieves the same outcome as
         *         #findActiveByEntityTypeAndEntityId(String, Long,
         *         but uses Spring DatAdditionalInfoJPA's derived query mechanism
         *         instead of a
         *         custom @Query annotation.
         */
        List<AdditionalInfo> findByEntityTypeAndEntityIdAndActiveTrue(
                        String entityType,
                        Long entityId);

        // Select all records from the AdditionalInfo table where the fieldValue
        // contains the search keyword, no matter where it appears in the text Filters
        // only the rows whose fieldValue contains your keyword
        // (case-insensitive).
        /*
         * findAll() loads every record from the additional_info table into memory.
         * .stream().filter(...) filters them in Java (not in the database).
         * It works fine for testing and small datasets (like in your H2 database).
         * But for a real database with thousands of records, this would be slow and
         * memory-heavy. Big O time and space complexity.
         */
        @Query("SELECT AdditionalInfoFROM AdditionalInfo AdditionalInfo" +
                        "WHERE LOWER(a.fieldValue) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        // Connects the method parameter to the :keyword in the query.
        List<AdditionalInfo> searchByFieldValue(@Param("keyword") String keyword);

}
