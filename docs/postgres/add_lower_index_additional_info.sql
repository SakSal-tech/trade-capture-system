-- Postgres migration SQL: functional lower() index for additional_info.field_value
-- Purpose: accelerate case-insensitive equality and prefix searches that use LOWER(field_value)

--
-- 1) This file is intended for inclusion in the project's database migration pipeline (Liquibase/Flyway)
--    or for execution by the DBA during deployment to the production Postgres instance.
-- 2) The index helps queries that use LOWER(field_value) or ILIKE for prefix/equality searches.
--    It does NOT make leading-wildcard searches (LIKE '%term%') efficient; for those, use pg_trgm.
-- 3) Create the index during a low-traffic window to reduce impact on running systems.

-- Create the functional lower() index (idempotent if index already exists)
CREATE INDEX IF NOT EXISTS idx_ai_field_value_lower
  ON additional_info (lower(field_value));

-- 1) BEFORE applying index, capture baseline with EXPLAIN ANALYZE for representative query:
--    EXPLAIN ANALYZE
--    SELECT a.* FROM additional_info a
--    WHERE a.entity_type = 'TRADE'
--      AND a.field_name = 'SETTLEMENT_INSTRUCTIONS'
--      AND LOWER(a.field_value) LIKE LOWER(CONCAT('%', 'Euroclear', '%'));
-- 2) Apply index.
-- 3) Re-run same EXPLAIN ANALYZE and compare execution time and plan.

-- Optional: drop index (if rollback needed)
-- DROP INDEX IF EXISTS idx_ai_field_value_lower;
