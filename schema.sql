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

-- ============================================================
-- Users and refresh tokens for JWT authentication (001-jwt-rbac-auth)
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at DATETIME     NOT NULL,
    revoked    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    INDEX      idx_user_id (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 003-url-click-analytics: add click counter to urls table
-- ============================================================
ALTER TABLE urls ADD COLUMN click_count BIGINT NOT NULL DEFAULT 0;
