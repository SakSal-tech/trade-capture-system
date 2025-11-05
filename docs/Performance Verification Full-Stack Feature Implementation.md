# Performance Verification

## Completed

During this stage of development, I focused on improving the efficiency and scalability of the trade settlement functionality. I replaced the earlier approach of retrieving all records and filtering them in memory using `.stream().filter(...)` with focused JPA repository queries. These queries now delegate the filtering work to the database, which is far more efficient.

I also introduced compound and single-column indexes on the `AdditionalInfo` table to support faster lookups. The compound index on `(entity_type, field_name, entity_id)` supports direct lookups of trade-related additional information, while the single index on `field_value` improves the performance of text searches within settlement instructions.

In addition, I moved business logic such as validation and filtering from the controller into the service layer. This change makes the controllers lighter and allows the database to handle the more complex operations through optimised SQL rather than in-memory Java processing.

I kept the settlement instruction field optional but ensured that when it is provided, it is validated before saving. The audit trail was also placed in a separate, lightweight table, which prevents unnecessary load on the main `AdditionalInfo` table and keeps data retrieval responsive.

### Summary of Performance Enhancements

**Database Queries** Moved from `findAll().stream().filter(...)` to focused SQL queries using `@Query` in the repository Avoids loading entire tables into memory; allows the database to filter results efficiently using indexes. Reduces time complexity from O(n) to approximately O(log n).
**Indexes** Added a compound index on `(entity_type, field_name, entity_id)` and a single index on `field_value` Enables the database to locate relevant rows quickly without scanning the entire table. Particularly improves keyword-based searches in settlement instructions.

### Production index guidance functional lower() index (detailed)

Context:

- The previous index on `field_value` improves equality and prefix searches, but does not help searches that use a leading wildcard (for example `LIKE '%term%'`).
- For case-insensitive searches where the repository query uses `LOWER(field_value)` or the application uses `ILIKE`, a functional index on `lower(field_value)` in Postgres can make equality and prefix queries much faster and is simple to add.

Recommendation:

- Create a functional index on `lower(field_value)` in the production database to accelerate case-insensitive equality and prefix searches (e.g., `LOWER(field_value) LIKE 'term%'`).
- For true substring searches (`%term%`), consider a trigram GIN index (`pg_trgm`) instead; that is documented in the notes below but the primary requested change is the `lower()` index.

Why a functional `lower()` index helps:

- When repository queries use `LOWER(a.fieldValue) LIKE LOWER(CONCAT('%', :keyword, '%'))`, databases that support functional indexes can map the `LOWER(field_value)` expression to a stored index. This avoids computing LOWER(...) across the entire table for matching and can use the index when the pattern does not start with a wildcard. For leading-wildcard searches (`%term%`), functional `lower()` alone will not help use trigram.

SQL to create the functional index (Postgres):

```sql
-- Create a functional index on lower(field_value)
-- This helps queries using LOWER(field_value) for equality or prefix matches.
CREATE INDEX IF NOT EXISTS idx_ai_field_value_lower
	ON additional_info (lower(field_value));
```

How to apply in production:

- Add the SQL above to the database migration tooling used by the project (Liquibase or Flyway). If migration tooling is not in use, provide the SQL to the DBA and include it in deployment runbooks.
- The index creation is non-destructive; it can be executed while the application is running but will temporarily consume CPU and IO during the index build.

How to verify the index is used (smoke test using Postgres):

1. Run an EXPLAIN ANALYZE on a representative query before creating the index to capture baseline execution time and plan:

```sql
EXPLAIN ANALYZE
SELECT a.* FROM additional_info a
WHERE a.entity_type = 'TRADE'
	AND a.field_name = 'SETTLEMENT_INSTRUCTIONS'
	AND LOWER(a.field_value) LIKE LOWER(CONCAT('%', 'Euroclear', '%'));
```

2. Create the functional index (run migration).
3. Re-run the same EXPLAIN ANALYZE and compare the execution plan and timing. If the plan shows an index scan (or bitmap index scan) using `idx_ai_field_value_lower`, query latency should improve for equality/prefix searches.

Notes and trade-offs:

- Functional lower() index works best for equality and prefix queries (e.g., `term%`). Leading wildcard searches (`%term%`) will still be expensive with B-tree/functional indexes. For those, add a trigram GIN index using the `pg_trgm` extension.
- Indexes increase storage usage and slow down writes (inserts/updates) to the indexed column. Settlement instructions updates are expected to be infrequent, so the read performance benefit typically outweighs the write cost.
- Tests run against H2 during CI will not reflect production Postgres indexing behaviour. Treat H2 tests as functional; add staging validation on a Postgres instance to confirm index impact.

Suggested follow-up (optional): provide a Liquibase changeSet that adds the index and a short verification script. See `docs/postgres/add_lower_index_additional_info.sql` for copy-paste SQL and comments.
**Service Layer Delegation** Moved filtering logic from controller to repository queries Reduces memory load in the controller and leverages JPA/Hibernate’s built-in query optimisation, improving execution time and scalability.
**Optional Field Validation** Only validates and saves non-null settlement instructions Reduces unnecessary operations and database writes, leading to faster and more efficient data handling.
**Audit Trail** Introduced a separate, lightweight table to store change history Keeps the `AdditionalInfo` table smaller and more responsive for real-time queries, while still satisfying the auditability requirement.

## Learned

I learned that moving filtering logic to SQL significantly improves performance because the database can use indexes and optimised search algorithms rather than relying on Java to iterate through entire collections. This change reduces the time complexity from a linear O(n) operation to something closer to logarithmic O(log n), depending on the indexing structure of the database.

I also learned that indexes should be applied strategically. Well-designed indexes improve performance, but unnecessary ones can slow down insert and update operations. The chosen indexes directly support the most common queries, such as retrieving settlement instructions and searching by keyword.

By validating only non-empty fields, the system avoids performing redundant work, improving responsiveness when managing large data volumes. Separating audit logs from live data also ensures that operational performance is maintained without compromising data traceability.

## Verification

I verified performance improvements by reviewing SQL query execution times in the logs and testing with realistic sample data. The refactored code now performs targeted database queries rather than loading all records into memory, resulting in noticeably faster response times for settlement instruction searches.

Memory usage remained consistent, as the application no longer retrieves entire datasets. I also confirmed that the new indexes did not cause a measurable slowdown in insert or update operations. The system now performs efficiently, scales better with increasing data, and satisfies the assignment’s performance verification requirement.
