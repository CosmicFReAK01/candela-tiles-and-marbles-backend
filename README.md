# Candela Tiles & Marbles — Backend Modular Monolith

> High-Performance Spring Boot 3.2.5 Modular Monolith for Candela Tiles & Marbles Atelier & Trade Backoffice.

---

## 🏛️ Architectural Overview

The backend is engineered as an enterprise-grade **Modular Monolith** in Java 17 and Spring Boot 3.2.5, replacing legacy microservice sprawl with single-deployable operational simplicity, in-memory domain boundaries, and sub-millisecond inter-module communication.

```
                                  HTTP / JSON (REST)
                                           │
                         ┌─────────────────┴─────────────────┐
                         │   Next.js 14 Frontend / Clients   │
                         └─────────────────┬─────────────────┘
                                           │
                  ┌────────────────────────▼────────────────────────┐
                  │          Spring Boot Modular Monolith           │
                  │                                                 │
                  │   ┌─────────────────────────────────────────┐   │
                  │   │ Security Filter Chain & Gateway Guard   │   │
                  │   │ • CSRF Origin Filter                    │   │
                  │   │ • Stateless JWT Auth Filter (HMAC-SHA)  │   │
                  │   │ • In-Memory IP Brute-Force Rate Limiter │   │
                  │   └────────────────────┬────────────────────┘   │
                  │                        │                        │
                  │    ┌───────────────────┴───────────────────┐    │
                  │    │           Bounded Domains             │    │
                  │    │  ┌───────────────┐ ┌───────────────┐  │    │
                  │    │  │  Auth Domain  │ │Product Domain │  │    │
                  │    │  └───────────────┘ └───────────────┘  │    │
                  │    │  ┌───────────────┐ ┌───────────────┐  │    │
                  │    │  │Inventory Dom. │ │Enquiry Domain │  │    │
                  │    │  └───────────────┘ └───────────────┘  │    │
                  │    │  ┌───────────────┐ ┌───────────────┐  │    │
                  │    │  │ Media Domain  │ │Content Domain │  │    │
                  │    │  └───────────────┘ └───────────────┘  │    │
                  │    └───────────────────┬───────────────────┘    │
                  │                        │                        │
                  │   ┌────────────────────┴────────────────────┐   │
                  │   │      Spring Data JPA / Hibernate 6      │   │
                  │   │  • HikariCP Connection Pool (Max: 3-5)  │   │
                  │   │  • PostgreSQL Dialect & Opt. Locking    │   │
                  │   └────────────────────┬────────────────────┘   │
                  └────────────────────────┼────────────────────────┘
                                           │
                        ┌──────────────────┴──────────────────┐
                        │                                     │
             ┌──────────▼──────────┐               ┌──────────▼──────────┐
             │    PostgreSQL 17    │               │  Supabase Storage   │
             │ (Supabase / Render) │               │ (`candela-media`)   │
             └─────────────────────┘               └─────────────────────┘
```

---

## 📦 Bounded Contexts & Module Architecture

The monolith enforces strict domain isolation. **ArchUnit tests** automatically fail the build if any domain attempts direct cross-domain repository access.

| Domain | Responsibility | Concurrency & Security Controls |
| :--- | :--- | :--- |
| **`auth`** | Staff/Admin authentication, token refreshing, session invalidation. | Stateless JWT (HS256/512), BCrypt password hashing, refresh token rotation, rate-limited login. |
| **`product`** | Catalogue taxonomy (Categories, Collections, Products, Search). | Slugs, multi-attribute indexing, optimistic locking (`@Version`). |
| **`inventory`** | Warehouse stock tracking, batch reorders, stock adjustments. | Optimistic concurrency control rejecting stale updates with `409 Conflict`. |
| **`enquiry`** | Trade quotes, sample enquiries, client lead tracking. | Bot honeypot traps, idempotency deduplication keys, SHA-256 hashed tracking tokens, zero PII leakage on public lookups. |
| **`media`** | Asset ingestion, cloud storage uploads, URL imports. | Magic bytes validation, MIME allowlisting (rejects SVG scripts), transactional orphan DB cleanup. |
| **`content`** | Storefront CMS, hero banners, curated collections. | Atomic updates, public cacheable read endpoints. |

---

## 🔒 Security Hardening & Zero-Trust Policies

1. **JWT Secret Fail-Fast:**
   The `JwtTokenProvider` requires a cryptographically strong secret ($\ge 32$ characters / 256 bits). Application startup immediately terminates if `JWT_SECRET` is missing, blank, or insecure. Zero-padding of weak keys is prohibited.
2. **Ephemeral Testing Credentials:**
   Test suites utilize `EphemeralJwtKeyInitializer` to generate a 512-bit `SecureRandom` key dynamically in-memory at context bootstrap. Zero hardcoded secrets exist in repository test YAML files.
3. **CSRF Origin Filter:**
   Mutating requests (`POST`, `PUT`, `DELETE`, `PATCH`) using session cookies require an origin matching the strict `ALLOWED_ORIGINS` whitelist.
4. **PII Scrubbing on Tracking:**
   Enquiry lookup endpoints enforce token verification. Public responses return stage statuses (`RECEIVED`, `CONTACTED`, `DISPATCHED`) while scrubbing customer email, phone numbers, and notes. Unauthenticated requests return identical 404 responses to eliminate enumeration attacks.
5. **Least-Privilege Database Access:**
   Configured for PostgreSQL connection pooling via HikariCP with connection validation and automatic SSL requirements (`sslmode=require`).

---

## ⚙️ Environment Variables

Copy `.env.example` to `.env` for local execution or configure in your deployment platform (e.g., Render):

| Variable | Required | Default | Description |
| :--- | :---: | :---: | :--- |
| `PORT` | No | `8080` | HTTP port for the Spring Boot application |
| `DATABASE_URL` | **Yes** | — | PostgreSQL connection string or JDBC URL |
| `DATABASE_USERNAME` | No | `candela_web` | Database user (if not in URL) |
| `DATABASE_PASSWORD` | No | — | Database password (if not in URL) |
| `DB_MAX_POOL_SIZE` | No | `3` | Maximum HikariCP pool connections |
| `HIBERNATE_DDL_AUTO` | No | `none` | Hibernate DDL mode (`none` in production) |
| `JWT_SECRET` | **Yes** | — | Cryptographic HMAC-SHA key ($\ge 32$ characters) |
| `JWT_EXPIRATION_MS` | No | `900000` | Access token lifespan (default: 15 min) |
| `ALLOWED_ORIGINS` | No | `http://localhost:3000` | Comma-separated CORS allowed origins |
| `SUPABASE_URL` | No | — | Supabase project URL for object storage |
| `SUPABASE_SERVICE_ROLE_KEY` | No | — | Service role key for media uploads |
| `SUPABASE_STORAGE_BUCKET` | No | `candela-media` | Target bucket name |

---

## 🛠️ Build & Development

### Prerequisites
- **Java 17** (OpenJDK 17 recommended)
- **Maven 3.9+**

### Compile & Run Tests
```bash
# Execute full unit, regression, security, and ArchUnit test suite
mvn clean test
```

### Run Locally
```bash
# Run application with active profile
export DATABASE_URL="postgresql://user:pass@host:5432/dbname?sslmode=require"
export JWT_SECRET="$(openssl rand -base64 64)"
mvn spring-boot:run
```

---

## 🐳 Docker Deployment

A hardened multi-stage `Dockerfile` is provided for zero-dependency container deployment:

```bash
# Build production Docker image
docker build -t candela-backend:latest .

# Run container as non-root unprivileged user
docker run -p 8080:8080 \
  -e DATABASE_URL="postgresql://user:pass@host:5432/dbname?sslmode=require" \
  -e JWT_SECRET="your-32-char-minimum-production-jwt-secret" \
  candela-backend:latest
```

---

## 🧪 Automated Test Suite

- **`SecurityTests`**: Validates JWT authentication, CSRF origin verification, brute-force IP rate limiting, and fail-fast secret verification.
- **`ArchitectureTests`**: ArchUnit checks verifying zero cross-domain direct repository couplings.
- **`OptimisticLockingTest`**: Verifies product and inventory version conflict resolution.
- **`EnquiryAndSearchTest`**: Verifies honeypot bot trap, idempotency caching, and PII-scrubbed enquiry tracking.
- **`MediaUploadFailureTest`**: Verifies rejection of executable/SVG payloads and cleanup of uncommitted media assets.
