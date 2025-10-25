package com.technicalchallenge.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * The AdditionalInfo entity represents flexible, key-value metadata
 * that can be linked to *any* type of entity in the system
 * (e.g. Trade, Counterparty, Book, etc.).
 *
 * Instead of modifying existing database tables each time new
 * business fields are needed, this table can hold them dynamically e.g:
 * entityType = "TRADE"
 * entityId = 1234
 * fieldName = "SETTLEMENT_INSTRUCTIONS"
 * fieldValue = "Settle via JPM New York, Account: 123456789"
 * fieldType = "STRING"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
// Refactored.ADDED Index for fast lookups, instead of reading every single row
// in the table one
// by one, to see if it matches.An index combining these three columns
// entity_type, field_name, which accelerates most of repository searches.
// Another index on field_value especially with case-insensitive matching
// queries. Big O That makes the average lookup O(log N), compared to O(N) for
// an unindexed full scan. for queries such as
// findActiveByEntityTypeAndEntityIdAndFieldName(...).
@Table(name = "additional_info", indexes = {
        @jakarta.persistence.Index(name = "idx_ai_entity_type_name_id", columnList = "entity_type,field_name,entity_id"),
        @jakarta.persistence.Index(name = "idx_ai_field_value_lower_like", columnList = "field_value")// for endpoint
                                                                                                      // searchTradeSettlementByKeyword(...).
})
public class AdditionalInfo {

    /**
     * @Id: marks this as the primary key.
     * @GeneratedValue: lets the database auto-generate it (auto-increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long additionalInfoId;

    /**
     * The type of entity this information belongs to.This
     * allows the same AdditionalInfo table to serve
     * multiple entity types. e.g:
     * - "TRADE" for trade-related metadata
     * - "COUNTERPARTY" for client details
     * - "BOOK" for accounting book information
     */
    @Column(name = "entity_type", nullable = false) // must not be null.
    private String entityType;

    /**
     * Foreign key (reference to another entity)
     * Identifies which record in another table (e.g. Trade) this info belongs to:
     * entityType = "TRADE"
     * entityId = 5678
     * means this AdditionalInfo record belongs to Trade ID 5678.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * The name of the field being stored. fieldName is the key part of a key:value
     * pair.
     * It tells the api what kind of extra information this record represents
     * "SETTLEMENT_INSTRUCTIONS", "DELIVERY_ADDRESS", "TAX_RATE"
     */
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    /**
     * The actual data value for the field.
     * 
     * This uses columnDefinition = "TEXT" so it can hold longer strings.
     * "Settle via JPM New York, Account: 123456789, Further Credit: ABC Corp"
     */
    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    /**
     * The type of data stored in fieldValue so I can add validations
     */
    @Column(name = "field_type", nullable = false)
    private String fieldType;

    /**
     * Indicates whether this record is active.
     * If set to false, the info is logically "deleted" or deactivated
     * without physically removing it from the database.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * The date and time when this record was first created.
     * Automatically initialized to the current timestamp when the object is
     * created.
     */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    /**
     * The date and time when this record was last modified.
     * Automatically updated before each update (see @PreUpdate below).
     */
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    /**
     * If the record is deactivated, this captures when that happened.
     * Example: when active = false, set deactivatedDate = LocalDateTime.now()
     */
    @Column(name = "deactivated_date")
    private LocalDateTime deactivatedDate;

    // Refactored ADDED: first version of record for audit trail so ti tracks how
    // many times a record changed. Prevents overwriting older updates by mistake if
    // multiple users edit simultaneously.
    @Version // Enables optimistic locking. Prevent two users from accidentally overwriting
             // each other's updates
    @Column(name = "version", nullable = false)
    private Integer version = 1;

}
