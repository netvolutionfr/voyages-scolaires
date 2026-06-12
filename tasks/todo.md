# Todo : Migration JWT cookies HttpOnly

## ✅ Terminé

- [x] **0.1** `AppProps.Cookie` record + properties (`app.cookie.secure/same-site/domain`)
- [x] **0.2** `CookieFactory` component (`access_token` / `refresh_token` / clear)
- [x] **1.1** CORS fixé — `CorsConfigurationSource` bean dev (remplace `WebMvcConfigurer` fragile)
- [x] **1.2** `CookieBearerTokenResolver` — cookie puis header Authorization ; câblé dans les deux filter chains
- [x] **2.1** `POST /api/webauthn/authenticate/finish` → Set-Cookie + `AuthCookieResponse`
- [x] **2.2** `POST /api/auth/refresh` → lit `refresh_token` cookie, réémet les deux cookies + `RefreshCookieResponse`
- [x] **2.3** `POST /api/otp/verify` → Set-Cookie + `RefreshCookieResponse`
- [x] **2.4** `POST /api/webauthn/register/finish*` → cookie `access_token` PENDING (pas de refresh token à ce stade)
- [x] **3.1** `POST /api/auth/logout` — révoque le refresh token, efface les deux cookies (MaxAge=0)
- [x] **4.1** `RestExceptionHandler` — `RuntimeException("unauthorized:…")` → 401

## À faire côté serveur (prod)

- [ ] Supprimer `ACTIVATION_JWT_SECRET` de l'env serveur (plus utilisé depuis migration ES256)
- [ ] Vérifier que le reverse proxy en prod transmet `Set-Cookie` sans le modifier
- [ ] Si le front et le back ne sont pas sur le même domaine en prod : activer CORS dans `prodSecurityChain` et configurer `COOKIE_DOMAIN`

---

# Audit de sécurité — 2026-06-12

Rapport complet : [`security-audit-2026-06-12.md`](security-audit-2026-06-12.md)

## ✅ Terminé — HIGH (4/4)

- [x] **H1** Fuite du reason code dans les erreurs de refresh (`UnauthorizedException`)
- [x] **H2** Refresh token OTP orphelin (`issue(user)` au lieu de `generateOpaqueToken()`)
- [x] **H3** `POST /api/trips/registrations` non protégé (`@PreAuthorize STUDENT/PARENT`)
- [x] **H4** `GET /api/documents/{docId}/preview` non protégé (`@PreAuthorize isAuthenticated`)
- [x] **H5** `IllegalArgumentException` fuitait `e.getMessage()` vers le client

## ✅ Terminé — MEDIUM (7/7)

- [x] **M1** Race condition OTP double émission (`@Lock PESSIMISTIC_WRITE`)
- [x] **M2** Claim `amr` codé en dur à `["webauthn"]` pour les sessions OTP
- [x] **M3** `GET /api/sections` sans `@PreAuthorize`
- [x] **M4** `GET /api/country` et `POST /api/trip-preferences` sans `@PreAuthorize`
- [x] **M5** `GET /api/me/documents` sans `@PreAuthorize`
- [x] **M6** Dualité `role`/`roles` dans `JwtAuthenticationConverter`
- [x] **M7** `new SecureRandom()` instancié à chaque chiffrement dans `CryptoConverter`

## ✅ Terminé — LOW (4/5)

- [x] **L1** TEACHER peut modifier n'importe quel voyage (`@tripSecurity.canViewTrip`)
- [~] **L2** TEACHER peut créer un voyage — **intentionnel**, comportement conservé
- [x] **L3** Paramètre `tripId` forgeable sur `PATCH /api/trips/registrations/{tripUserId}`
- [x] **L4** `JwksController` injectait l'`ECKey` complet (clé privée incluse)
- [x] **L5** `ImportController` loguait le nom de fichier contrôlé par le client

## ✅ Terminé — INFO (2/2)

- [x] **I2** HSTS ajouté dans `prodSecurityChain` (`includeSubDomains`, `maxAgeInSeconds=31536000`) — s'active sur les requêtes HTTPS directes ; déléguer au reverse proxy si TLS est terminé en amont
- [x] **I3** `Content-Disposition` encodé RFC 6266 / RFC 5987 dans `DocumentPreviewStreamController` — fallback ASCII (`filename="…"`) + param UTF-8 percent-encoded (`filename*=UTF-8''…`), caractères de contrôle et CRLF assainis
