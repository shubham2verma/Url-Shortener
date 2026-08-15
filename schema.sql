-- ============================================================
-- URL Shortener — MySQL Schema
-- Run this once before starting the application.
-- Compatible with MySQL 5.x and 8.x
-- ============================================================

CREATE DATABASE IF NOT EXISTS url_shortener
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE url_shortener;

CREATE TABLE IF NOT EXISTS urls (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    long_url   VARCHAR(2048) NOT NULL,
    short_code VARCHAR(20)   NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_short_code (short_code),
    INDEX       idx_long_url  (long_url(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
