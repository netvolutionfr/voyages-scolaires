# Changelog

All notable changes to this project will be documented in this file.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)  
Versioning: [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

---

## [Unreleased] — 2026-06-12 — Security Audit & Hardening

Full structural and semantic security audit (Security by Design) covering the
authentication flow, privilege isolation, and exception resilience. All findings
from severity levels HIGH → MEDIUM → LOW have been resolved.

See [`tasks/security-audit-2026-06-12.md`](tasks/security-audit-2026-06-12.md)
for the complete audit report with per-finding analysis.

### Security — HIGH fixes

- **Refresh token reason code leak** — `RefreshTokenServiceImpl.unauthorized()` no
  longer embeds the technical reason in the exception message. A dedicated
  `UnauthorizedException` is thrown and mapped to a generic `{"error":"invalid_token"}`
  401 by `RestExceptionHandler`. The reason is only logged server-side (`WARN`).
- **OTP refresh token orphan** — `OtpServiceImpl.verifyAccountOtp` now calls
  `refreshTokenService.issue(user)` (persists hash + familyId in DB) instead of
  the bare `generateOpaqueToken()`. Silent disconnection on first refresh for OTP
  sessions is fixed.
- **Unprotected `POST /api/trips/registrations`** — endpoint restricted to
  `STUDENT` and `PARENT` via `@PreAuthorize`. TEACHER and ADMIN roles can no
  longer self-register.
- **Unprotected `GET /api/documents/{docId}/preview`** — `@PreAuthorize("isAuthenticated()")`
  added as a Spring Security guard independent of the in-method ownership check.
- **`IllegalArgumentException` information leak** — `RestExceptionHandler` no
  longer forwards `e.getMessage()` to the client (was exposing credential IDs and
  internal identifiers). Returns a generic `"Invalid request"` with internal `WARN`
  logging.

### Security — MEDIUM fixes

- **OTP double-issue race condition** — `OtpTokenRepository` exposes a new
  `findLatestPendingForUpdate` method annotated with `@Lock(PESSIMISTIC_WRITE)`.
  All three call sites in `OtpServiceImpl` (`issueAndSend`, `verifyAccountOtp`,
  `resend`) use this locked query, eliminating the concurrent-emit window.
- **`amr` claim hardcoded to `["webauthn"]`** — `JwtService` gains an
  `generateAccessToken(User, List<String> amr)` overload as the canonical method.
  WebAuthn sessions pass `["webauthn"]`; OTP sessions now correctly pass `["otp"]`.
  A `default` wrapper preserves backward compatibility for the refresh rotation path.
- **`GET /api/sections` and `GET /api/sections/{id}` unguarded** — both GET
  endpoints annotated with `@PreAuthorize("isAuthenticated()")`.
- **`GET /api/country` unguarded** — `@PreAuthorize("isAuthenticated()")` added.
- **`POST /api/trip-preferences/{voyageId}` unguarded** — `@PreAuthorize("isAuthenticated()")`
  added; no longer reachable by unauthenticated callers or PENDING tokens.
- **`GET /api/me/documents` unguarded** — `@PreAuthorize("isAuthenticated()")`
  added to `DocumentController`.
- **Dual `role`/`roles` claim ambiguity in `JwtAuthenticationConverter`** — the
  `roles` (Collection) branch removed; converter now reads only the `role` (String)
  claim emitted by the application. A crafted JWT with `roles:["ADMIN"]` no longer
  yields elevated authorities.
- **`CryptoConverter` per-call `SecureRandom` instantiation** — replaced with a
  `private static final SecureRandom SECURE_RANDOM` instance shared across all
  encryption calls.

### Security — LOW fixes

- **TEACHER can update any trip (`PUT /api/trips/{id}`)** — `@PreAuthorize`
  updated to `hasRole('ADMIN') or (hasRole('TEACHER') and @tripSecurity.canViewTrip(#id))`.
  A TEACHER can only modify trips they are assigned to as a chaperone.
- **`PATCH /api/trips/registrations/{tripUserId}` — forgeable `tripId` parameter**
  — `@RequestParam Long tripId` removed from the handler. Authorization now uses
  `@tripSecurity.canViewRegistration(#tripUserId)` which resolves the trip from the
  database through the registration record, preventing parameter forgery.
- **`JwksController` held a reference to the full `ECKey` (private key included)**
  — a new `JWKSet publicJwkSet(ECKey ecKey)` bean is registered in `JwtConfig` and
  injects only the public JWK into `JwksController`. A future serialization mistake
  can no longer expose the private key.
- **`ImportController` logged user-controlled filename (log injection)** —
  `file.getOriginalFilename()` replaced with `file.getSize()` in the log statement.

### Security — INFO fixes

- **HSTS added to `prodSecurityChain`** — `Strict-Transport-Security: max-age=31536000; includeSubDomains`
  emitted on HTTPS responses. Note: if TLS is terminated by a reverse proxy, configure
  HSTS there and remove this block from the application.
- **`Content-Disposition` filename encoded per RFC 6266 / RFC 5987** in
  `DocumentPreviewStreamController` — control characters (including CRLF), quotes,
  and backslashes stripped from the ASCII fallback (`filename="…"`); full UTF-8
  name emitted as `filename*=UTF-8''<percent-encoded>` for compatible browsers.

---

## [0.4.0] — 2026-06 — JWT HttpOnly Cookie Migration

Tokens moved from `localStorage` (XSS-readable) to `HttpOnly` cookies
(`access_token` / `refresh_token`). See [`tasks/plan.md`](tasks/plan.md).

### Added
- `CookieFactory` — builds `ResponseCookie` objects (HttpOnly, Secure, SameSite
  from `app.cookie.*` properties).
- `CookieBearerTokenResolver` — reads the access token from the `access_token`
  cookie, falls back to `Authorization` header.
- `AuthCookieResponse` / `RefreshCookieResponse` DTOs — no longer echo tokens in
  the JSON body.
- `POST /api/auth/logout` — revokes the refresh token and clears both cookies
  (`MaxAge=0`).

### Changed
- `SecurityConfig` — `CookieBearerTokenResolver` wired into both filter chains;
  CORS migrated from `WebMvcConfigurer` to a `CorsConfigurationSource` bean (dev
  profile only).
- `AuthController.refresh` — reads `refresh_token` cookie, re-issues both cookies.
- `OtpController.verify` — emits `Set-Cookie` headers.
- `RestExceptionHandler` — `RuntimeException("unauthorized:…")` mapped to HTTP 401.

---

## [0.3.0] — 2026-05 — JWT Algorithm Migration (HS256 → ES256)

### Security
- JWT signing migrated from HS256 (symmetric HMAC) to ES256 (ECDSA P-256,
  asymmetric). Private key signs; public key verifies.
- `kid` header added to every JWS (RFC 7638 thumbprint) to support key rotation.
- Public JWK exposed at `GET /.well-known/jwks.json`.
- EC P-256 key pair stored in `secrets/jwt/` (gitignored, permissions 700/600).

---

## [0.2.0] — Previous — Critical & Medium Security Fixes

### Security (from git history)
- Fix two critical security issues (`ff6dc5e`)
- Fix four medium security issues (`c78b2ff`)

---

## [0.1.0] — Previous — Bootstrap

- Spring Boot 3.5 REST API, Java 21
- WebAuthn/Passkeys authentication with OTP fallback
- PostgreSQL + Flyway migrations
- S3/MinIO document storage with per-document DEK encryption
- Field-level encryption via `CryptoConverter` (AES/GCM, `CRYPTO_KEK_B64`)
- Roles: `ADMIN`, `PARENT`, `STUDENT`, `TEACHER`
