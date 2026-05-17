# Plan : Migration JWT LocalStorage → cookies HttpOnly

## Contexte

Les JWT étaient renvoyés en JSON body et stockés en LocalStorage côté front, accessible à JavaScript (XSS). Migration vers cookies HttpOnly (non lisibles en JS) conformément à OWASP.

## Spec

1. Login émet `Set-Cookie` HttpOnly ; Secure ; SameSite=Strict au lieu du body JSON
2. `/api/auth/refresh` lit le refresh token depuis le cookie et réémet les cookies
3. CORS credentials:true avec origine explicite (bean `CorsConfigurationSource`)
4. Access token accepté via cookie OU header `Authorization` (via `CookieBearerTokenResolver`)

## Architecture des cookies

| Cookie | Path | Durée |
|--------|------|-------|
| `access_token` | `/` | 15 min (JWT) |
| `refresh_token` | `/api/auth` | 30 jours |

Dev : `Secure=false`, `SameSite=Lax`  
Prod : `Secure=true`, `SameSite=Strict`

## Fichiers créés

| Fichier | Rôle |
|---------|------|
| `application/service/CookieFactory.java` | Fabrique les `ResponseCookie` (HttpOnly, Secure, SameSite depuis config) |
| `web/CookieBearerTokenResolver.java` | Lit l'access token depuis cookie ou header Authorization |
| `infrastructure/dto/authentication/AuthCookieResponse.java` | Réponse login WebAuthn (sans token en clair) |
| `infrastructure/dto/authentication/RefreshCookieResponse.java` | Réponse OTP verify + refresh |

## Fichiers modifiés

| Fichier | Changement |
|---------|------------|
| `config/AppProps.java` | Ajout record `Cookie(secure, sameSite, domain)` |
| `application.properties` | Ajout `app.cookie.*` |
| `application-dev.properties` | Override `secure=false`, `sameSite=Lax` |
| `config/SecurityConfig.java` | CORS via `CorsConfigurationSource` bean, `CookieBearerTokenResolver` dans les deux chains |
| `web/AuthController.java` | `finishAuthn`, `refresh`, `finishRegister*` → Set-Cookie ; nouveau `POST /api/auth/logout` |
| `web/OtpController.java` | `verify()` → Set-Cookie |
| `web/RestExceptionHandler.java` | `RuntimeException("unauthorized:…")` → 401 |

## Vérification

```bash
# 1. Build
./gradlew compileJava

# 2. Login → check Set-Cookie headers
curl -v POST /api/webauthn/authenticate/finish ... | grep -i set-cookie

# 3. Requête protégée via cookie
curl -b "access_token=<token>" /api/trips  # → 200

# 4. Refresh via cookie
curl -v -X POST /api/auth/refresh -b "refresh_token=<token>"  # → 200 + nouveaux cookies

# 5. Logout
curl -v -X POST /api/auth/logout -b "refresh_token=<token>"  # → 204 + MaxAge=0

# 6. CORS preflight
curl -v -X OPTIONS /api/trips -H "Origin: http://localhost:5173"
# → Access-Control-Allow-Credentials: true
```
