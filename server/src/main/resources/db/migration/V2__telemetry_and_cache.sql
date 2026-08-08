CREATE TABLE requests (
    id                UUID NOT NULL,
    api_key_id        UUID NOT NULL,
    org_id            UUID NOT NULL,
    provider          TEXT NOT NULL,
    model             TEXT NOT NULL,
    status            INT NOT NULL,
    prompt_tokens     INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    cost_micros       BIGINT NOT NULL DEFAULT 0,  
    latency_ms        INT NOT NULL,
    ttft_ms           INT,                        
    cache_hit         BOOLEAN NOT NULL DEFAULT false,
    failover          BOOLEAN NOT NULL DEFAULT false,
    error_code        TEXT,
    correlation_id    TEXT,
    created_at        TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_requests_key_time ON requests(api_key_id, created_at DESC);

-- Daily partitions for today (2026-08-08) and the next 7 days.
-- Created by hand for now; a scheduled job takes this over in S-37.
-- The deliberate gap past 2026-08-16 means a far-future insert fails loudly
-- rather than silently landing somewhere wrong.
CREATE TABLE requests_2026_08_08 PARTITION OF requests
    FOR VALUES FROM ('2026-08-08') TO ('2026-08-09');
CREATE TABLE requests_2026_08_09 PARTITION OF requests
    FOR VALUES FROM ('2026-08-09') TO ('2026-08-10');
CREATE TABLE requests_2026_08_10 PARTITION OF requests
    FOR VALUES FROM ('2026-08-10') TO ('2026-08-11');
CREATE TABLE requests_2026_08_11 PARTITION OF requests
    FOR VALUES FROM ('2026-08-11') TO ('2026-08-12');
CREATE TABLE requests_2026_08_12 PARTITION OF requests
    FOR VALUES FROM ('2026-08-12') TO ('2026-08-13');
CREATE TABLE requests_2026_08_13 PARTITION OF requests
    FOR VALUES FROM ('2026-08-13') TO ('2026-08-14');
CREATE TABLE requests_2026_08_14 PARTITION OF requests
    FOR VALUES FROM ('2026-08-14') TO ('2026-08-15');
CREATE TABLE requests_2026_08_15 PARTITION OF requests
    FOR VALUES FROM ('2026-08-15') TO ('2026-08-16');

CREATE TABLE usage_hourly (
    api_key_id   UUID NOT NULL,
    org_id       UUID NOT NULL,
    hour         TIMESTAMPTZ NOT NULL,
    model        TEXT NOT NULL,
    requests     BIGINT NOT NULL DEFAULT 0,
    prompt_tokens     BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    cost_micros  BIGINT NOT NULL DEFAULT 0,
    cache_hits   BIGINT NOT NULL DEFAULT 0,
    errors       BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (api_key_id, hour, model)
);

CREATE TABLE cache_entries (
    id           UUID PRIMARY KEY,
    org_id       UUID NOT NULL,
    model        TEXT NOT NULL,
    prompt_hash  TEXT NOT NULL,         -- exact-match fast path
    embedding    vector(768) NOT NULL,
    response     JSONB NOT NULL,
    prompt_tokens     INT NOT NULL,
    completion_tokens INT NOT NULL,
    hit_count    BIGINT NOT NULL DEFAULT 0,
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cache_embedding ON cache_entries
    USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_cache_hash ON cache_entries(prompt_hash, model);
