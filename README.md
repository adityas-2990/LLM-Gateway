# LLM Gateway

A self-hosted gateway that sits between your applications and the LLM providers they call.
Applications point at one OpenAI-compatible endpoint and authenticate with a gateway-issued
API key; the gateway handles provider routing and failover, per-key rate limiting and spend
budgets, a semantic response cache, and per-request usage and cost metering. An admin API
and dashboard cover key management and usage analytics.

Built with Java 25, Spring Boot, PostgreSQL + pgvector, and Redis.

## How to run

> Not runnable end to end yet — the build skeleton is in place (S-01) and local
> infrastructure lands in S-02. These are the commands as they will work.

```bash
# 1. Start Postgres and Redis
docker compose up -d

# 2. Run the gateway against them
cd server
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. Check it is alive
curl localhost:8080/actuator/health
```

Requires JDK 25 and Docker. Copy `.env.example` to `.env` and fill in provider keys before
making real provider calls.

## Layout

| Path | Contents |
|---|---|
| `server/` | Spring Boot gateway |
| `LLM_Gateway_Project_Spec.md` | Design spec |
| `STORIES.md` | Story backlog, one story per session |
