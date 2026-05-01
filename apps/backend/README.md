# TMS Backend — Spring Boot 3.3

Spring Boot monolith for the TMS Parking Validation system. Handles auth, multi-tenant data isolation, validation sessions, QR links, notifications, reports, and audit logs.

---

## Tech Stack

| Concern | Library |
|---|---|
| Framework | Spring Boot 3.3, Java 21 virtual threads |
| Database | PostgreSQL 16 via Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Cache | Redis (Spring Data Redis) |
| Messaging | RabbitMQ (Spring AMQP) |
| Auth | Auth0 JWKS validation (Nimbus JOSE JWT) |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Reports | Apache POI (Excel), iText 7 (PDF), OpenCSV |
| QR | ZXing |
| Storage | AWS SDK v2 S3 |
| Notifications | Twilio SMS, AWS SES email |

---

## Running Locally

### Prerequisites

- Java 21
- Docker (for postgres, redis, rabbitmq)

### 1. Start infrastructure

From the project root:

```bash
make up
make ps   # wait until all 3 show (healthy)
```

### 2. Configure environment

The backend reads from environment variables. Defaults for local dev are baked into `application.yml`. For secrets, set them in your shell or a `.env` file:

```bash
export AUTH0_DOMAIN=dev-xxx.us.auth0.com
export AUTH0_CLIENT_ID=your-client-id
export AUTH0_CLIENT_SECRET=your-client-secret
export QR_HMAC_SECRET=$(openssl rand -hex 32)
export AWS_REGION=ca-central-1
export AWS_S3_BUCKET=tms-reports
```

### 3. Run

```bash
./mvnw spring-boot:run
```

Or from the project root:

```bash
make dev
```

The server starts on **http://localhost:8080**

---

## Key Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/actuator/health` | None | Health check (all components) |
| GET | `/api/auth/login` | None | Returns Auth0 PKCE authorize URL |
| POST | `/api/auth/callback` | None | Exchange code for tokens |
| POST | `/api/auth/refresh` | None | Rotate refresh token |
| GET | `/api/auth/me` | JWT | Current user |
| GET | `/api/v1/validations` | JWT | List sessions (paginated, filtered) |
| POST | `/api/v1/validations` | JWT | Create session |
| POST | `/api/v1/validations/public/{token}` | None | QR scan — public |
| GET | `/api/v1/links` | JWT | List QR links |
| POST | `/api/v1/links` | JWT | Generate QR link |
| GET | `/api/v1/links/{id}/qr-pdf` | JWT | Download QR PDF |
| POST | `/api/v1/reports` | JWT | Queue report generation |
| GET | `/api/v1/reports/{id}` | JWT | Poll report status |
| GET | `/api/v1/audit-logs` | JWT | Audit log (read-only) |
| GET | `/api/v1/users/me` | JWT | Current user profile + role |
| PUT | `/api/v1/users/{id}/role` | JWT (ADMIN) | Assign role |

Full API docs: **http://localhost:8080/swagger-ui.html**

---

## Running Tests

```bash
./mvnw test
# or from project root:
make test
```

Tests use Testcontainers — Docker must be running. Each module has:
- `@WebMvcTest` per controller (mocked services)
- Cross-tenant isolation test (real Postgres via Testcontainers)

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5433` | PostgreSQL port |
| `DB_NAME` | `tms_parking` | Database name |
| `DB_USERNAME` | `tms_user` | Database user |
| `DB_PASSWORD` | `tms_password` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USER` | `guest` | RabbitMQ username |
| `RABBITMQ_PASS` | `guest` | RabbitMQ password |
| `AUTH0_DOMAIN` | — | Auth0 tenant domain |
| `AUTH0_CLIENT_ID` | — | Auth0 application client ID |
| `AUTH0_CLIENT_SECRET` | — | Auth0 application client secret |
| `AUTH0_NAMESPACE` | `https://tms.cpa.ca` | JWT custom claims namespace |
| `APP_BASE_URL` | `http://localhost:3000` | Frontend URL (CORS + QR links) |
| `QR_HMAC_SECRET` | — | 32+ char secret for HMAC-SHA256 QR tokens |
| `AWS_REGION` | `ca-central-1` | AWS region |
| `AWS_S3_BUCKET` | `tms-reports` | S3 bucket for reports and logos |
| `AWS_SES_FROM_EMAIL` | `noreply@cpa.com` | SES sender address |
| `TWILIO_ACCOUNT_SID` | — | Twilio account SID (optional) |
| `TWILIO_AUTH_TOKEN` | — | Twilio auth token (optional) |
| `TWILIO_FROM_NUMBER` | — | Twilio sender number (optional) |

---

## Package Structure

```
ca.cpa.tms/
├── TmsApplication.java
├── shared/          # TenantContext, ApiError, exceptions, interceptors, RabbitMQ/Redis config
├── auth/            # JwtAuthenticationFilter, Auth0Adapter, SecurityConfig, AuthController
├── tenant/          # Client, Tenant, SubTenant, Zone, QuotaConfig entities + controllers
├── user/            # User entity, role cache, UserController
├── validation/      # ValidationSession, quota enforcement, expiry scheduler
├── qrlink/          # ValidationLink, HMAC tokens, QR PDF generator
├── notification/    # RabbitMQ listeners, Twilio SMS, AWS SES, WebSocket push
├── report/          # ReportJob, async worker, S3 upload, presigned URLs
└── audit/           # AuditLog (append-only), AuditController
```

---

## Building a Docker Image

```bash
docker build -t tms-backend .
docker run -p 8080:8080 --env-file .env tms-backend
```

Or use docker-compose from the project root:

```bash
make fresh   # builds and starts all 5 containers
```
