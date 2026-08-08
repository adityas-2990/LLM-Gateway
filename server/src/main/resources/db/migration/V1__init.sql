CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE organizations (
    id                   UUID PRIMARY KEY,
    name                 TEXT NOT NULL,
    monthly_budget_cents BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    org_id        UUID NOT NULL REFERENCES organizations(id),
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role          TEXT NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY,
    org_id          UUID NOT NULL REFERENCES organizations(id),
    name            TEXT NOT NULL,
    key_hash        TEXT NOT NULL UNIQUE,   -- SHA-256 of the full key
    prefix          TEXT NOT NULL,          -- 'gw_live_a3f2' for display
    scopes          TEXT[] NOT NULL DEFAULT '{chat}',
    rate_limit_rpm  INT NOT NULL DEFAULT 60,
    budget_cents    BIGINT NOT NULL DEFAULT 0,   -- 0 = unlimited
    cache_enabled   BOOLEAN NOT NULL DEFAULT true,
    cache_threshold REAL NOT NULL DEFAULT 0.95,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_keys_hash ON api_keys(key_hash) WHERE revoked_at IS NULL;


CREATE TABLE model_pricing (
    model                 TEXT PRIMARY KEY,
    provider              TEXT NOT NULL,
    prompt_micros_per_1k     BIGINT NOT NULL,
    completion_micros_per_1k BIGINT NOT NULL
);