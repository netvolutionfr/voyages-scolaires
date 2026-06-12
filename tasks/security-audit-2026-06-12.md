# Security Audit — 2026-06-12

Structural and semantic audit of the "Voyages" Spring Boot API against
Security by Design criteria. Three focus axes: authentication flow, privilege
isolation, and exception resilience.

## Summary

| Severity | Found | Fixed |
|----------|-------|-------|
| CRITICAL | 0     | —     |
| HIGH     | 4     | 4     |
| MEDIUM   | 7     | 7     |
| LOW      | 5     | 4 + 1 intentional |
| INFO     | 3     | noted |

---

## HIGH — All fixed

### H1 — Refresh token reason code leak
**Files:** `RefreshTokenServiceImpl.java`, `RestExceptionHandler.java`  
**Problem:** `unauthorized(String reason)` embedded the technical reason
(`reused_refresh_revoked_family`, `expired_refresh`, `invalid_refresh`) directly
in the `RuntimeException` message, which was forwarded to the HTTP client via
`RestExceptionHandler`. An attacker could fingerprint token state (unknown vs.
expired vs. revoked family).  
**Fix:** New `UnauthorizedException` (domain layer, no message). `unauthorized()`
now logs the reason at WARN and throws this typed exception. Dedicated
`@ExceptionHandler(UnauthorizedException.class)` returns `{"error":"invalid_token"}`
with HTTP 401.

### H2 — OTP refresh token orphan (silent session break)
**Files:** `OtpServiceImpl.java:134`  
**Problem:** `verifyAccountOtp` called `refreshTokenService.generateOpaqueToken()`
(generates random bytes only) instead of `issue(user)`. The token was never
persisted in `refresh_tokens`. Any subsequent `POST /api/auth/refresh` call from
an OTP-authenticated session returned 401 — users were silently locked out after
15 minutes.  
**Fix:** Changed to `refreshTokenService.issue(user)`. The token is now hashed,
stored with `familyId` and `expiresAt`, and participates in the rotation protocol.

### H3 — Unprotected `POST /api/trips/registrations`
**Files:** `TripRegistrationController.java`  
**Problem:** The `create()` method had no `@PreAuthorize`. Any authenticated role
(including TEACHER, ADMIN) could self-register for a trip. Only the `/**` catch-all
rule applied.  
**Fix:** `@PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")` added.

### H4 — Unprotected `GET /api/documents/{docId}/preview`
**Files:** `DocumentPreviewStreamController.java`  
**Problem:** No `@PreAuthorize`. Access control was entirely delegated to
in-method logic (ownership check). A bug in that logic would leave the endpoint
unguarded at the Spring Security layer.  
**Fix:** `@PreAuthorize("isAuthenticated()")` added as the minimum Spring
Security guard.

### H5 — `IllegalArgumentException` message forwarded to client
**Files:** `RestExceptionHandler.java`, `AuthenticationServiceImpl.java`  
**Problem:** `handleIllegalArgument` returned `e.getMessage()` in the response
body. `AuthenticationServiceImpl` threw `"Unknown credential ID: [b1, b2, ...]"`,
allowing an attacker to confirm credential IDs in the database.  
**Fix:** Handler now returns the generic `"Invalid request"` string; message is
logged at WARN server-side only.

---

## MEDIUM — All fixed

### M1 — OTP double-issue race condition
**Files:** `OtpTokenRepository.java`, `OtpServiceImpl.java`  
**Problem:** Concurrent `POST /api/otp/resend` requests could both pass the
cooldown check before either committed, resulting in two valid OTP codes for the
same user simultaneously.  
**Fix:** `findLatestPendingForUpdate` added to `OtpTokenRepository` with
`@Lock(LockModeType.PESSIMISTIC_WRITE)`. All call sites in `OtpServiceImpl`
(`issueAndSend`, `verifyAccountOtp`, `resend`) updated to use this locked query.

### M2 — `amr` claim hardcoded to `["webauthn"]` for OTP sessions
**Files:** `JwtService.java`, `JwtServiceImpl.java`, `AuthenticationServiceImpl.java`, `OtpServiceImpl.java`  
**Problem:** `generateAccessToken` always emitted `amr: ["webauthn"]` regardless
of the authentication method. OTP sessions incorrectly claimed a higher-assurance
method, potentially granting elevated trust to integrations that read `amr`
(RFC 8176).  
**Fix:** `generateAccessToken(User, List<String> amr)` added as the canonical
method. WebAuthn path passes `List.of("webauthn")`; OTP path passes `List.of("otp")`.
A `default` wrapper on the interface preserves backward compatibility for the
refresh rotation path.

### M3 — `GET /api/sections` unguarded
**Files:** `SectionController.java`  
**Fix:** `@PreAuthorize("isAuthenticated()")` on both `list()` and `getSectionById()`.

### M4 — `GET /api/country` and `POST /api/trip-preferences` unguarded
**Files:** `CountryController.java`, `TripPreferenceController.java`  
**Fix:** `@PreAuthorize("isAuthenticated()")` on both endpoints.

### M5 — `GET /api/me/documents` unguarded
**Files:** `DocumentController.java`  
**Fix:** `@PreAuthorize("isAuthenticated()")` added.

### M6 — Dual `role`/`roles` claim — privilege escalation surface
**Files:** `SecurityConfig.java`  
**Problem:** `JwtAuthenticationConverter` read both `role` (String, emitted by the
app) and `roles` (Collection, never emitted but accepted). A crafted JWT containing
`roles: ["ADMIN"]` would be granted ADMIN authority.  
**Fix:** The `roles` Collection branch removed. Converter reads only `role` (String).

### M7 — `CryptoConverter` — per-call `SecureRandom` instantiation
**Files:** `CryptoConverter.java`  
**Problem:** `new SecureRandom()` was called inside `convertToDatabaseColumn` on
every field write, causing unnecessary entropy seeding cost under concurrent DB
access.  
**Fix:** `private static final SecureRandom SECURE_RANDOM = new SecureRandom()`
shared instance.

---

## LOW — 4 fixed, 1 intentional

### L1 — TEACHER can update any trip (FIXED)
**Files:** `TripController.java`  
**Problem:** `PUT /api/trips/{id}` allowed any TEACHER to modify any trip,
regardless of chaperone assignment.  
**Fix:** `@PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and @tripSecurity.canViewTrip(#id))")`

### L2 — TEACHER can create trips (INTENTIONAL)
**Files:** `TripController.java`  
**Decision:** Confirmed intentional — TEACHERs are allowed to create new trips.
No change applied.

### L3 — Forgeable `tripId` on `PATCH /api/trips/registrations/{tripUserId}` (FIXED)
**Files:** `TripRegistrationController.java`  
**Problem:** `@PreAuthorize` used `@tripSecurity.canViewTrip(#tripId)` where
`tripId` came from `@RequestParam` (client-controlled). A TEACHER assigned to
trip A could forge `?tripId=A` while targeting a registration from trip B via
`tripUserId`.  
**Fix:** `@RequestParam Long tripId` removed. `@PreAuthorize` now uses
`@tripSecurity.canViewRegistration(#tripUserId)` which resolves the trip ID from
the database through the registration record.

### L4 — `JwksController` held full `ECKey` including private key (FIXED)
**Files:** `JwtConfig.java`, `JwksController.java`  
**Problem:** `JwksController` was injected with the full `ECKey` bean (private key
present). Although `toPublicJWK()` was called correctly, a future change could
accidentally expose the private key in the JWKS response.  
**Fix:** New `JWKSet publicJwkSet(ECKey ecKey)` bean in `JwtConfig` pre-computes
the public-only JWKSet at startup. `JwksController` now injects `JWKSet` directly
— the private key is structurally inaccessible from the controller.

### L5 — `ImportController` logged user-controlled filename (FIXED)
**Files:** `ImportController.java`  
**Problem:** `log.info("Received file for import: {}", file.getOriginalFilename())`
logged a value fully controlled by the HTTP client, creating a log injection
surface (CRLF, fake log entries).  
**Fix:** Replaced with `log.info("Received file for import, size={} bytes", file.getSize())`.

---

## INFO — Noted, no code change

| # | Finding | Status |
|---|---------|--------|
| I1 | `RequestAuditFilter` logs `sub` and `jti` in clear — `@Profile("dev")` only, no production risk | Accepted |
| I2 | No HTTP security headers (HSTS, CSP) configured in `SecurityConfig` — API only (no HTML rendered); HSTS should be added when TLS termination moves to the app tier | Backlog |
| I3 | `Content-Disposition` filename in `DocumentPreviewStreamController` not RFC 5987-encoded — aesthetic/compat issue, no security impact in current usage | Backlog |

---

## Positive findings (confirmed robust)

- Refresh token rotation with family invalidation (RFC 6819 §5.2.2.3) — pessimistic lock, SHA-256 storage, `ROTATED`/`REVOKED` status chain.
- JWT ES256 + `kid` RFC 7638 — `toPublicJWK()` called correctly; private key not exposed.
- AES/GCM field encryption — 12-byte random IV per ciphertext, 128-bit tag, IV+ciphertext concatenated.
- OTP hashed with BCrypt — no plaintext storage, constant-time comparison via `matches()`.
- WebAuthn with `userVerificationRequired = true` — biometric/PIN mandatory.
- Double OTP protection — sliding-window quota + per-token cooldown.
- Dev/prod profile separation — Swagger, open CORS, and audit logging isolated to `dev`.
