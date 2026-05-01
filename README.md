# TMS Parking Validation

Multi-tenant SaaS platform for the Calgary Parking Authority (CPA) to validate vehicle license plates without payment. Operators scan a QR code, enter a plate number, and the system creates a timed validation session.

**Hierarchy:** Client → Tenant → Sub-Tenant

---

## Architecture

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 21, Maven |
| Frontend | Next.js 15, React 19, MUI v6 |
| Database | PostgreSQL 16 (Flyway migrations) |
| Cache | Redis 7 (sessions, role cache, PKCE state) |
| Queue | RabbitMQ 3.13 (notifications, report generation) |
| Auth | Auth0 (PKCE + server-side refresh token rotation) |
| Storage | AWS S3 (generated reports, tenant logos) |

---

## Prerequisites

| Tool | Minimum version |
|---|---|
| Docker + Docker Compose | 24+ |
| Java | 21 |
| Node.js | 18.18+ |
| Maven | 3.9+ (or use the included `./mvnw`) |

---

## Quick Start — Local Development

### 1. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and fill in the required secrets:

| Variable | Where to get it |
|---|---|
| `AUTH0_DOMAIN` | Auth0 Dashboard → tenant domain (e.g. `dev-xxx.us.auth0.com`) |
| `AUTH0_CLIENT_ID` | Auth0 Dashboard → Applications → your app |
| `AUTH0_CLIENT_SECRET` | Auth0 Dashboard → Applications → your app |
| `QR_HMAC_SECRET` | Run: `openssl rand -hex 32` |
| `NEXTAUTH_SECRET` | Run: `openssl rand -hex 32` |
| `AWS_ACCESS_KEY_ID` | AWS IAM console |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM console |

### 2. Start infrastructure

```bash
make up     # starts postgres, redis, rabbitmq
make ps     # verify all 3 containers show (healthy)
```

### 3. Start backend

```bash
make dev
# or directly:
cd apps/backend && ./mvnw spring-boot:run
```

Wait for: `Started TmsApplication in X seconds`

### 4. Start frontend

```bash
cd apps/frontend
npm install
npm run dev
```

The frontend reads from `apps/frontend/.env.local` — see [apps/frontend/README.md](apps/frontend/README.md) for its setup.

### 5. Open in browser

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ console | http://localhost:15672 — guest / guest |
| PostgreSQL | localhost:5433 — db=`tms_parking`, user=`tms_user` |
| Redis | localhost:6379 |

---

## Running Everything with Docker

Builds and starts all 5 containers (postgres, redis, rabbitmq, backend, frontend):

```bash
make fresh    # clean slate — wipes volumes, rebuilds images, starts all 5
make ps       # verify all 5 containers show (healthy)
```

Open http://localhost:3000 once all containers are healthy.

---

## Auth0 Setup

1. Create a **Regular Web Application** in your Auth0 tenant
2. Under **Settings**, configure:

**Allowed Callback URLs**
```
http://localhost:3000/api/auth/callback/auth0
```

**Allowed Logout URLs**
```
http://localhost:3000
```

**Allowed Web Origins**
```
http://localhost:3000
```

3. Under **Advanced Settings → Grant Types**, enable:
   - Authorization Code
   - Refresh Token

4. Enable **Refresh Token Rotation** under **Advanced Settings → OAuth**

---

## Makefile Reference

| Target | Description |
|---|---|
| `make up` | Start infrastructure containers |
| `make down` | Stop all containers |
| `make fresh` | Destroy volumes and restart clean |
| `make logs` | Tail all container logs |
| `make ps` | Show container status and health |
| `make test` | Run backend test suite (`mvn test`) |
| `make dev` | Run backend with hot-reload |

---

## Project Structure

```
parking-validation-system/
├── apps/
│   ├── backend/          # Spring Boot monolith
│   │   ├── src/main/java/ca/cpa/tms/
│   │   │   ├── auth/         # JWT filter, Auth0 adapter, security config
│   │   │   ├── audit/        # Append-only audit log
│   │   │   ├── notification/ # RabbitMQ listeners, SMS, email, WebSocket
│   │   │   ├── qrlink/       # QR code generation, HMAC token signing
│   │   │   ├── report/       # Async CSV/Excel/PDF report generation
│   │   │   ├── shared/       # Tenant context, exceptions, global handler
│   │   │   ├── tenant/       # Clients, tenants, zones, sub-tenants, quota
│   │   │   ├── user/         # User management, role cache
│   │   │   └── validation/   # Validation sessions, expiry scheduler
│   │   └── src/main/resources/
│   │       ├── application.yml
│   │       └── db/migration/ # Flyway SQL scripts
│   └── frontend/         # Next.js 15 app
│       └── src/
│           ├── app/          # App Router pages
│           ├── components/   # Shared MUI components
│           ├── hooks/        # useCurrentUser, etc.
│           ├── lib/          # apiClient (axios + silent refresh)
│           ├── providers/    # SessionProvider, QueryClient, ThemeProvider
│           ├── store/        # Zustand tenant/branding store
│           └── types/        # TypeScript API DTOs
├── infra/
│   ├── docker-compose.yml
│   └── postgres/init.sql
├── docs/                 # Architecture docs and build plan
├── .env.example
└── Makefile
```

---

## Seed Data (fixed UUIDs)

| Entity | ID | Name |
|---|---|---|
| Client | `11111111-1111-1111-1111-111111111111` | CPA Demo Client |
| Tenant 1 | `22222222-2222-2222-2222-222222222222` | Downtown Parking |
| Tenant 2 | `33333333-3333-3333-3333-333333333333` | Airport Parking |

---

## Test Users

Create these users in Auth0 and insert matching rows in the `users` table with the correct `auth0_user_id`:

| Email | Role | Access |
|---|---|---|
| admin@cpa.com | ADMIN | Everything — all clients and tenants |
| client@cpa.com | CLIENT_ADMIN | All tenants within their client |
| tenant@cpa.com | TENANT_ADMIN | Zones, links, reports, branding for their tenant |
| subtenant@cpa.com | SUBTENANT_USER | Validations and links only |
