-- ============================================================
-- BUSINESS MANAGER - ONE-TIME DATABASE BOOTSTRAP
-- Creates the application database `business_manager` and a
-- dedicated least-privilege application user.
--
-- Run ONCE as a MySQL administrator:
--   mysql -u root -p < database/setup.sql
--
-- Safe to re-run. No retailpro database or user is referenced.
-- ============================================================

CREATE DATABASE IF NOT EXISTS business_manager
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Dedicated application user (least privilege: only business_manager)
CREATE USER IF NOT EXISTS 'business_manager_app'@'localhost' IDENTIFIED BY 'Bm!Local2026_Db9x';
ALTER USER 'business_manager_app'@'localhost' IDENTIFIED BY 'Bm!Local2026_Db9x';
ALTER USER 'business_manager_app'@'localhost' WITH MAX_USER_CONNECTIONS 20;

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES
    ON business_manager.* TO 'business_manager_app'@'localhost';
FLUSH PRIVILEGES;

-- Schema and seed data are applied automatically by DatabaseInitializer
-- on first launch (database/schema.sql), or manually:
--   mysql -u business_manager_app -p business_manager < database/schema.sql
