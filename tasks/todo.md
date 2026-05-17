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
