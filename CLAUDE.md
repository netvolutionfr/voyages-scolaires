# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run locally (dev profile)
./gradlew bootRun --no-daemon --args='--spring.profiles.active=dev'

# Build executable JAR
./gradlew bootJar

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "fr.siovision.voyages.SomeTest"

# Start dependencies (PostgreSQL + MinIO)
docker-compose up -d
```

Test reports are generated in `build/reports/tests/`.

Swagger UI is available at `http://localhost:8080/swagger-ui.html` in dev mode only.

## Architecture

Spring Boot 3.5 REST API using Java 21. Layered architecture:

```
fr.siovision.voyages/
├── web/               # REST controllers
├── application/
│   ├── service/       # Business logic interfaces + impls
│   ├── security/      # Encryption utilities, JWT helpers
│   ├── aspect/        # AOP logging (dev-only)
│   └── listeners/     # Domain event listeners
├── domain/
│   ├── model/         # JPA entities
│   └── events/        # Domain events
├── infrastructure/
│   ├── repository/    # Spring Data JPA repositories
│   ├── dto/           # Request/Response DTOs
│   ├── mapper/        # MapStruct mappers
│   └── converter/     # JPA attribute converters (field encryption)
└── config/            # Spring Security, JWT, WebAuthn, S3 configs
```

## Key Design Decisions

**Authentication flow:** WebAuthn/Passkeys (primary) → OTP fallback → JWT access tokens (15min) + refresh token rotation (30 days). JWT is ES256-signed (ECDSA P-256, asymmetric); refresh tokens are tracked in DB with family-based invalidation. Private key signs; public key verifies. The public JWK is exposed at `GET /.well-known/jwks.json`. `kid` is included in every JWS header (RFC 7638 thumbprint) to support key rotation.

**Field-level encryption:** Sensitive user fields (`gender`, `phone`, `birthDate`) are transparently encrypted/decrypted via JPA converters (`CryptoConverter`, `CryptoDateConverter`) using the `CRYPTO_KEK_B64` environment variable.

**Document storage:** Files go to S3/MinIO. The S3 object key and an encrypted DEK (Data Encryption Key) are stored in the `documents` table. Each document has a SHA256 checksum, MIME type, and expiration.

**Profiles:**
- `dev`: CORS open to `localhost:3000/5173/8000`, Swagger enabled, request audit logging via `RequestAuditFilter`, AOP method logging via `LoggingAspect`
- `prod`: Swagger disabled, CORS handled by reverse proxy, graceful shutdown enabled

**Roles:** `ADMIN`, `PARENT`, `STUDENT`, `TEACHER` — stored as a `Set<Role>` on the `User` entity and embedded in JWT `roles` claim.

## Database

PostgreSQL with Flyway migrations in `src/main/resources/db/migration/`. Versioned migrations use `V{n}__name.sql`; repeatable ones use `R__name.sql` (lookup tables, test data).

## Environment Variables

A `.env` file at the project root is used locally. Minimum required:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/voyages
SPRING_DATASOURCE_USERNAME=voyages
SPRING_DATASOURCE_PASSWORD=changeit
SPRING_PROFILES_ACTIVE=dev
APP_FRONT_URL=http://localhost:5173
WEBAUTHN_ALLOWED_ORIGINS=http://localhost:5173
WEBAUTHN_RP_ID=localhost
WEBAUTHN_DEFAULT_ORIGIN=http://localhost:5173
JWT_PRIVATE_KEY=<PKCS8 PEM EC P-256 private key, \n-escaped — see secrets/jwt/>
JWT_PUBLIC_KEY=<SPKI PEM EC P-256 public key, \n-escaped — see secrets/jwt/>
JWT_ISSUER=https://voyages.local
S3_ENDPOINT=http://localhost:9000
S3_BUCKET=voyages
S3_ACCESS_KEY=...
S3_SECRET_KEY=...
SMTP_HOST=...
SMTP_USER=...
SMTP_PASS=...
CRYPTO_KEK_B64=<base64-encoded-key>
```

## Key Management

EC P-256 key pair stored locally in `secrets/jwt/` (gitignored, permissions 700/600).

### Generate a new key pair
```bash
mkdir -p secrets/jwt && chmod 700 secrets/jwt
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out secrets/jwt/ec-private.pem
openssl ec -in secrets/jwt/ec-private.pem -pubout -out secrets/jwt/ec-public.pem
chmod 600 secrets/jwt/ec-private.pem

# Format for .env (collapse newlines to literal \n)
python3 -c "
with open('secrets/jwt/ec-private.pem') as f:
    print('JWT_PRIVATE_KEY=' + f.read().strip().replace('\n', '\\\\n'))
with open('secrets/jwt/ec-public.pem') as f:
    print('JWT_PUBLIC_KEY=' + f.read().strip().replace('\n', '\\\\n'))
"
```

### Key rotation
1. Generate a new key pair (above)
2. Update `JWT_PRIVATE_KEY` + `JWT_PUBLIC_KEY` in all environments
3. Restart — the `kid` in `/.well-known/jwks.json` changes automatically
4. In-flight access tokens (≤15 min) with the old `kid` are rejected; users re-authenticate
