# Security-critical backend audit — 2026-09-04

## Scope and method

This review was intentionally limited to authentication, authorization and role
boundaries, object-level access (IDOR/BOLA), documents and health data, secrets,
logging, Spring Security configuration, dependencies, and regression coverage.
It was a source review plus automated tests; it was not a penetration test of a
deployed environment. Findings from the earlier 2026-06-12 audit were rechecked
where they intersected this scope.

## Confirmed vulnerabilities (fixed)

### C1 — Any teacher could decrypt any document (HIGH)

`GET /api/documents/{docId}/preview` treated the `TEACHER` role itself as
sufficient authorization. A teacher who obtained another document's UUID could
therefore bypass the trip/chaperone relationship enforced by the separate admin
preview endpoint and stream the decrypted document. UUID entropy reduces
discovery but is not authorization.

**Fix:** teacher access now delegates to `TripSecurity.canPreviewDocument`, which
requires a trip linking the teacher, student, and required document type. Owners
and administrators retain their existing access. The check happens before key
unwrapping or object-storage access. A regression test proves an unrelated
teacher receives 403 and storage is not read.

### C2 — Authentication secrets and personal data written to development logs (HIGH)

The development `LoggingAspect` serialized every controller argument and return
value. This included OTP verification codes, WebAuthn registration/authentication
payloads, access tokens, refresh-related responses, health forms, and GDPR
exports. Development logs are still durable/exfiltratable data and frequently
reach shared log collectors.

**Fix:** the aspect now records only HTTP method, path, controller method,
duration, and error message. It never reads controller arguments or stringifies
responses. A regression test uses a response whose `toString()` fails and also
verifies `getArgs()` is never called.

## Likely risks (not changed without product/deployment confirmation)

1. **Presigned cover uploads have only a broad role gate.** `/api/files/presign`
   allows every application role at the filter chain; `mode=cover` does not
   perform an ownership or trip check. If cover images are trip-managed content,
   students and parents can obtain write URLs. Confirm the intended uploader
   roles, then enforce them at method level.
2. **Upload validation trusts the multipart `Content-Type`.** The encrypted upload
   flow allowlists the caller-supplied MIME value but does not inspect file magic,
   scan for malware, or validate PDF/image structure. Stored objects are served
   inline, increasing the consequence of type confusion. Add server-side type
   detection and malware scanning before marking a document ready.
3. **Local object-storage credentials have insecure fallbacks.** `minioadmin` is a
   useful local default, but a production deployment missing `S3_ACCESS_KEY` or
   `S3_SECRET_KEY` silently starts with known credentials. Move defaults to the
   dev profile and require both properties elsewhere.
4. **Public authentication endpoints rely on application-level throttling.** OTP
   issuance has per-account controls, but no edge/IP/device rate limit is visible
   for WebAuthn challenge creation, registration attempts, or refresh requests.
   Add gateway limits and monitoring; avoid IP as the sole key.
5. **Development audit logs retain stable identifiers.** `RequestAuditFilter`
   records username, JWT subject, and JTI. It masks bearer headers and is dev-only,
   but shared development logging may still create unnecessary personal-data
   retention. Prefer a short-lived correlation ID and pseudonymous actor ID.

## Absent or unimplemented controls/features

- No malware/content-disarm pipeline is present for identity and travel documents.
- No explicit parent/guardian delegated document-read policy exists on the stream
  endpoint; current behavior is owner/admin/assigned-teacher only. This is not a
  vulnerability unless guardian access is a requirement.
- No automated dependency-vulnerability scan or lockfile verification is defined
  in Gradle. Version pinning exists, but it is not equivalent to CVE monitoring.
- No deployed dynamic test verified proxy/TLS behavior, cookie behavior, S3 bucket
  policy, or whether development profiles/logging are excluded in production.

## Confirmed positive controls

- Production API requests default to authenticated role-bearing JWTs; Swagger is
  disabled in production and actuator binds to loopback with only health/info
  public.
- JWT verification is restricted to ES256 and validates the configured issuer;
  authority mapping accepts only the application's singular `role` claim.
- Refresh tokens are stored as hashes and rotated with family revocation; OTPs are
  BCrypt-hashed, attempt-limited, expiring, and issued under a locked transaction.
- Sensitive database fields and document bodies use authenticated AES-GCM
  encryption; document keys are wrapped separately.
- Admin document and health-form endpoints use trip relationship checks rather
  than trusting a client-provided user or trip identifier alone.
- Refresh cookies default to `Secure`, `HttpOnly`, and `SameSite=Strict`; production
  CORS is disabled. The development overrides are profile-scoped.

## Recommendations and patch validation

1. Add integration tests with method security enabled for every document/health
   endpoint and a role/object matrix (owner, guardian, assigned teacher,
   unrelated teacher, admin).
2. Remove production credential fallbacks and validate security properties at
   startup with `@ConfigurationProperties` constraints.
3. Add an OWASP Dependency-Check, OSV Scanner, or equivalent CI gate, with a
   documented suppression-review process.
4. Exercise the deployed stack through its reverse proxy: verify HSTS, cookie
   flags, rejected cross-origin state changes, actuator isolation, S3 private
   bucket policy, URL expiry, and absence of sensitive values in logs.
5. Re-run the full test suite and clean build for every security patch. The two
   fixes in this audit each include a focused regression test.
