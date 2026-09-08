# Vayvora AI Calling — Backend API

Spring Boot microservices for the Vayvora AI Calling platform, built against
the product and technical documentation.

**Status:** schema and REST surface are complete and compiling. The voice
pipeline itself (SIP, STT, TTS, LLM) is not implemented — see
[What is not built](#what-is-not-built).

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5, Spring Cloud Gateway 2023.0.3 |
| Database | PostgreSQL |
| Build | Maven (multi-module) |
| Auth | JWT (HS256) with revocable refresh tokens |

## Modules

| Module | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8000 | Single entry point; validates tokens, forwards identity |
| `auth-service` | 8001 | Registration, login, token lifecycle, profile |
| `agent-service` | 8003 | AI agent configuration, versioning, publishing |
| `call-service` | 8004 | Call lifecycle, transcripts, call intelligence |
| `campaign-service` | 8005 | Campaigns, campaign contacts, CRM contacts |
| `analytics-service` | 8006 | Dashboard metrics, usage, billing |
| `knowledge-service` | 8007 | Knowledge bases, ingestion, retrieval |
| `shared` | — | Entities, repositories, JWT, tenant context, error handling |

## Getting started

### Prerequisites

- JDK 21 (Lombok does not yet support JDK 26 — the build fails on it)
- Maven 3.9+
- PostgreSQL 14+

### Setup

```bash
cp .env.example .env        # then edit .env with real values
```

Create the schema:

```bash
psql "$DATABASE_URL" -f db/migration/V1__initial_schema.sql
psql "$DATABASE_URL" -f db/migration/V2__reference_data.sql
```

Build and run:

```bash
mvn clean install -DskipTests

# Each service in its own terminal, gateway last
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar agent-service/target/agent-service-1.0.0.jar
java -jar call-service/target/call-service-1.0.0.jar
java -jar campaign-service/target/campaign-service-1.0.0.jar
java -jar analytics-service/target/analytics-service-1.0.0.jar
java -jar knowledge-service/target/knowledge-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

All requests go through the gateway at `http://localhost:8000`.

## API

Base path `/api/v1`. Every endpoint except register, login and refresh requires
`Authorization: Bearer <token>`.

### Auth

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/register` | Create an organization and its first admin |
| POST | `/auth/login` | Authenticate; returns access + refresh token |
| POST | `/auth/refresh-token` | Exchange a refresh token for a new access token |
| POST | `/auth/logout` | Revoke a refresh token |
| POST | `/auth/verify` | Token introspection |
| GET | `/auth/me` | Current user |
| PATCH | `/auth/me` | Update profile |
| POST | `/auth/change-password` | Change password; revokes all sessions |

### Agents

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/agents` | List agents (`?status=`, `?page=`, `?size=`) |
| POST | `/agents` | Create an agent |
| GET | `/agents/{id}` | Retrieve an agent with its current configuration |
| PUT | `/agents/{id}` | Update configuration |
| POST | `/agents/{id}/publish` | Publish the current version |
| POST | `/agents/{id}/archive` | Take the agent out of service |
| GET | `/agents/{id}/versions` | Version history |
| GET | `/agents/voices` | Available TTS voices |
| GET | `/agents/tools` | Tools this organization may enable |

### Calls

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/calls` | List calls (`?state=`, `?agentId=`) |
| GET | `/calls/live` | Calls currently in a live state |
| POST | `/calls` | Initiate a call |
| GET | `/calls/{id}` | Retrieve a call |
| GET | `/calls/{id}/detail` | Call plus every derived artifact |
| GET | `/calls/{id}/transcript` | Transcript and turns |
| PATCH | `/calls/{id}/state` | Advance the state machine |
| POST | `/calls/{id}/messages` | Append a transcript turn |
| POST | `/calls/{id}/intelligence` | Attach summary, sentiment, intent, lead score |

### Campaigns and contacts

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/campaigns` | List campaigns (`?state=`) |
| POST | `/campaigns` | Create a campaign |
| GET | `/campaigns/{id}` | Retrieve a campaign |
| GET | `/campaigns/{id}/metrics` | Aggregate outcomes |
| GET | `/campaigns/{id}/contacts` | Enrolled contacts and their progress |
| POST | `/campaigns/{id}/contacts` | Enrol contacts |
| POST | `/campaigns/{id}/start` | Start or resume |
| POST | `/campaigns/{id}/pause` | Pause |
| POST | `/campaigns/{id}/complete` | Mark complete |
| POST | `/campaigns/{id}/schedule` | Schedule (`?startAt=`) |
| GET | `/contacts` | List/search contacts (`?q=`, `?band=`) |
| POST | `/contacts` | Create a contact |
| GET | `/contacts/{id}` | Retrieve a contact |
| PATCH | `/contacts/{id}/do-not-call` | Set do-not-call (`?value=`) |

### Knowledge

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/knowledge-bases` | List knowledge bases |
| POST | `/knowledge-bases` | Create a knowledge base |
| GET | `/knowledge-bases/{id}` | Retrieve one |
| GET | `/knowledge-bases/stats` | Ingestion statistics |
| GET | `/knowledge-bases/{id}/documents` | List documents |
| POST | `/knowledge-bases/{id}/documents` | Add a document |
| POST | `/knowledge-bases/search` | Retrieve chunks for a query |
| POST | `/knowledge-documents/{id}/reingest` | Re-run ingestion |
| DELETE | `/knowledge-documents/{id}` | Delete a document |

### Analytics, usage, billing

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/analytics/dashboard` | Headline figures (`?days=`) |
| GET | `/analytics/trend` | Daily volume by direction |
| GET | `/analytics/sentiment` | Sentiment distribution |
| GET | `/analytics/topics` | Top intents by share |
| GET | `/analytics/agents` | Per-agent performance |
| GET | `/analytics/lead-bands` | Contact counts by band |
| GET | `/usage` | Metered usage for the period |
| GET | `/billing` | Plan and estimated charges |

## Design notes

**Tenant isolation.** The organization is the root entity; every tenant-scoped
table carries `organization_id`. Repositories take it as the first argument,
and there is deliberately no bare `findById` for tenant data — lookups go
through `findByIdAndOrganizationId`, so a caller cannot fetch another tenant's
row by guessing an id.

**Identity flow.** The gateway validates the bearer token once and forwards
`X-Vayvora-User-Id` / `X-Vayvora-Org-Id` downstream. It strips any
client-supplied copies of those headers first — without that, a caller could
set `X-Vayvora-Org-Id` by hand and read another tenant's data.

**Call state machine.** Transitions are validated in `CallService`, not
trusted from the caller. Dashboards, billing and transfer logic all read call
state, so a call jumping from RINGING to COMPLETED without connecting would
corrupt every one of them. `connected_at` is set on the transition to
CONNECTED because billing meters voice minutes from that instant.

**Agent versioning.** Configuration lives on `ai_agent_versions`, not the agent
row. Editing a published agent creates a new draft rather than mutating the
live one, so traffic keeps hitting the reviewed configuration until someone
publishes the replacement.

**Campaign metrics** are computed from `campaign_contacts` on read rather than
kept as counters, so a retried or corrected attempt cannot leave totals
inconsistent.

**Money** is stored in minor units (paise) as `BIGINT`. Never floating point.

**Secrets** are stored hashed: refresh tokens and API keys as SHA-256 digests,
passwords as BCrypt. Raw values are returned once and never persisted.

## What is not built

Stated plainly so nobody plans around a capability that is not there.

- **Voice pipeline.** SIP integration, speech-to-text, text-to-speech and the
  LLM turn loop (§8, §11) are not implemented. `POST /calls` records a call and
  advances its state; it does not dial anyone.
- **Embeddings.** Ingestion parses and chunks documents, but does not generate
  vectors. `POST /knowledge-bases/search` falls back to keyword matching so the
  retrieval path is exercisable end to end; scores it returns are positional,
  not semantic.
- **Kafka events.** Brokers are configured and the dependency is present, but
  no producers or consumers are wired up. Services talk to the database
  directly.
- **Redis.** Configured but unused; no caching or session state yet.
- **Binary uploads.** Documents are registered with inline text or a
  `sourceUri`; multipart upload to object storage is not implemented.
- **Webhook delivery.** Endpoints are stored; nothing dispatches to them.
- **Tests.** No test suite yet. The build compiles and the schema is
  consistent with the entities, but behaviour is unverified.

## Pricing note

The plans seeded in `V2__reference_data.sql` (₹999 Starter, ₹4,999 Business)
are the **illustrative** figures from the product documentation, which states
explicitly that they are not final commercial pricing. They must not be quoted
to customers.
