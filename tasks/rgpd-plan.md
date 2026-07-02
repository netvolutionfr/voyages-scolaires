# Plan RGPD — Droits d'accès, portabilité, rectification, effacement

**Date : 2026-07-02 — Statut : PLANIFICATION (aucun code écrit)**

Périmètre : backend uniquement. Le front sera adapté dans une étape ultérieure.

---

## 1. Inventaire des données personnelles (état des lieux)

| Table | Données personnelles | Chiffrement | FK → users | Cascade DB |
|---|---|---|---|---|
| `users` | email, nom, prénom, displayName, **gender** 🔐, **telephone** 🔐, **birthDate** 🔐, consentGivenAt, consentText | CryptoConverter | — | — |
| `student_health_form` | **payload santé complet** 🔐 (données Art. 9 — catégorie spéciale) | CryptoConverter | `student_id` NOT NULL | **aucune** |
| `documents` | originalFilename, fileNumber (n° passeport/CNI), objectKey (contient le publicId), DEK wrappée | corps chiffré AES-GCM dans S3 | `user_id` NOT NULL | **aucune** |
| `trip_user` | statut d'inscription, decisionMessage, adminNotes (texte libre) | — | `user_id` | JPA cascade=ALL depuis User |
| `trip_preferences` | intérêt pour un voyage | — | `user_id` | JPA cascade=ALL + orphanRemoval |
| `otp_tokens` | codeHash (BCrypt) | — | `user_id` NOT NULL | **aucune** |
| `refresh_tokens` | tokenHash, familyId, lastUsedAt | — | `user_id` NOT NULL + self-FK `replaced_by_id` | **aucune** |
| `web_authn_credential` | credentialId, coseKey, userHandle | — | `user_id` | **aucune** |
| `parent_child` | lien familial parent↔enfant | — | `parent_id`, `child_id` | **aucune** |
| `registration_attempt` | emailHint (fragment d'email, **sans FK**) | — | — | purge temporelle existante |
| `users.legal_guardian_user_id` | auto-référence tuteur légal | — | self-FK | **aucune** |

Constat critique : **`V1__init.sql` ne définit aucun `ON DELETE CASCADE`**, et le
`DELETE /api/users/{id}` admin existant appelle simplement `userRepository.delete(user)`
→ violation FK garantie pour tout utilisateur réel (documents, tokens, credentials…).
L'effacement RGPD doit donc être un **service orchestré**, pas un simple delete.

Danger annexe repéré : `Section.users` porte `cascade=ALL, orphanRemoval=true`
→ supprimer une Section supprimerait ses Users. À corriger dans cette itération.

---

## 2. Endpoints proposés

| Méthode | Route | Droit RGPD | @PreAuthorize |
|---|---|---|---|
| `GET` | `/api/me/data-export` | Accès (Art. 15) + Portabilité (Art. 20) | `isAuthenticated()` |
| `PATCH` | `/api/me/profile` | Rectification (Art. 16) | `isAuthenticated()` |
| `DELETE` | `/api/me` | Effacement (Art. 17) | `isAuthenticated()` + réauthentification récente |

Remarque : on reste sous `/api/me/**` — cohérent avec l'existant
(`GET /api/me`, `GET /api/me/documents`, `GET /api/me/health-form`).

---

## 3. Droit d'accès + portabilité — `GET /api/me/data-export`

### Contenu du JSON exporté

```jsonc
{
  "exportedAt": "2026-07-02T10:00:00Z",
  "format": "voyages-gdpr-export/v1",
  "profile": {
    "publicId", "email", "firstName", "lastName", "displayName",
    "gender", "birthDate", "telephone",          // déchiffrés (converters JPA)
    "role", "status", "section",
    "createdAt", "updatedAt"
  },
  "consent": { "givenAt", "text" },
  "legalGuardian": { "fullName", "email" },       // si présent
  "familyLinks": [ { "relation": "PARENT|CHILD", "fullName", "email" } ],
  "trips": {
    "registrations": [ { "tripLabel", "destination", "dates",
                         "registrationStatus", "registrationDate", "decisionDate",
                         "decisionMessage" } ],    // adminNotes EXCLU (données du tiers responsable)
    "preferences":   [ { "tripLabel", "interest" } ]
  },
  "documents": [ { "type", "originalFilename", "mime", "size", "sha256",
                   "fileNumber", "deliveryDate", "expirationDate", "createdAt" } ],
  "healthForm": { "signedAt", "validUntil", "payload": { /* JSON déchiffré */ } },
  "security": {
    "passkeys": [ { "createdAt", "aaguid" } ],     // pas de coseKey/credentialId
    "lastLoginAt": "…"                              // max(refresh_tokens.lastUsedAt)
  }
}
```

Exclusions volontaires : hashes de tokens, DEK/IV, `adminNotes`
(notes internes de l'établissement — Art. 15 ne couvre pas les données
d'appréciation interne pouvant léser des tiers ; à arbitrer avec le DPO).

### Implémentation

- **DTO** : `infrastructure/dto/gdpr/GdprExportResponse.java` + sous-records
  (`GdprProfile`, `GdprTripData`, `GdprDocumentMeta`, …). Ne PAS réutiliser
  `UserResponse` tel quel (il omet consent/createdAt) — mais s'en inspirer.
- **Service** : `application/service/GdprService.java` (interface) +
  `impl/GdprServiceImpl.java` — méthode `export(User user)`. Lecture seule,
  `@Transactional(readOnly = true)`. Agrège via les repositories existants
  (`DocumentRepository.findAllReadyByUser`, `StudentHealthFormRepository.findLatestByUserId`,
  `TripUserRepository`, `TripPreferenceRepository`, `ParentChildRepository`,
  `WebAuthnCredentialRepository.findByUser`, `RefreshTokenRepository`).
  Manque à ajouter : `ParentChildRepository.findByParentOrChild(User)`.
- **Controller** : `web/GdprController.java` —
  `Content-Type: application/json` +
  `Content-Disposition: attachment; filename="voyages-export-{publicId}.json"`
  (réutiliser le pattern RFC 5987 de `DocumentPreviewStreamController` si nom accentué —
  ici le nom est contrôlé serveur, pas de risque).
- Les **fichiers** (corps des documents) ne sont pas embarqués dans le JSON :
  ils restent téléchargeables un par un via `GET /api/documents/{docId}/preview`
  existant. Un export ZIP (JSON + fichiers déchiffrés) est possible en phase
  ultérieure si le DPO l'exige — coûteux en streaming, à ne pas faire d'emblée.
- **Rate-limiting simple** : l'export déchiffre tout le profil + le health form ;
  limiter à N exports / heure / utilisateur (réutiliser le pattern
  cooldown/sliding-window déjà en place pour l'OTP) pour éviter d'en faire
  un oracle de déchiffrement.

---

## 4. Droit de rectification — `PATCH /api/me/profile`

État actuel : seul `POST /api/me` (updateTelephone) existe en self-service ;
la rectification complète passe par `PUT /api/users/{id}` (ADMIN).

### Proposition à deux niveaux

1. **Self-service direct** (le user modifie lui-même) — champs « de contact » :
   `telephone`, `displayName`, `gender`.
   - DTO `ProfileUpdateRequest` (tous champs optionnels, semantics PATCH :
     absent = inchangé).
   - Bean Validation : format téléphone, `gender ∈ {M, F, N}`.
   - Implémentation dans `UserService.updateOwnProfile(User, ProfileUpdateRequest)`.
2. **Champs d'identité** (`firstName`, `lastName`, `birthDate`, `email`) —
   **PAS de modification self-service directe** : ce sont des données d'état
   civil contrôlées par l'établissement (dossier scolaire) et `email` est
   l'identifiant d'authentification (unicité + WebAuthn userHandle dérivé).
   → Le droit de rectification s'exerce via une **demande** :
   `POST /api/me/rectification-request` (body : champ + valeur souhaitée + motif),
   stockée en table `rectification_request` (nouvelle migration), visible des
   ADMIN qui appliquent via le `PUT /api/users/{id}` existant.
   *Alternative minimale si on veut éviter la table : simple mailto documenté
   côté front + traitement manuel. À trancher — la table est recommandée pour
   la traçabilité (Art. 12 : réponse sous 1 mois, il faut horodater).*

Déprécier ensuite `POST /api/me` (updateTelephone) au profit du PATCH.

---

## 5. Droit à l'effacement — `DELETE /api/me`

C'est le morceau le plus lourd. Trois sous-chantiers :

### 5.1 Migration Flyway `V{n}__gdpr_erasure_support.sql`

- Ajouter `ON DELETE CASCADE` sur les FK « purement techniques » :
  `otp_tokens.user_id`, `refresh_tokens.user_id`, `web_authn_credential.user_id`,
  `refresh_tokens.replaced_by_id` (→ `ON DELETE SET NULL` pour la chaîne de rotation).
- Ajouter `ON DELETE SET NULL` sur `users.legal_guardian_user_id`.
- **Ne PAS** mettre de cascade sur `documents.user_id` ni `student_health_form.student_id` :
  leur suppression exige une action applicative (S3, traçabilité santé) —
  une cascade silencieuse masquerait un bug. Le service les traite explicitement.
- Corriger le mapping `Section.users` (retirer `cascade=ALL, orphanRemoval=true`)
  — changement JPA, pas SQL.

### 5.2 Service `UserErasureService`

`@Transactional` — ordre de suppression :

1. **Garde-fous** (refus HTTP 409 avec code d'erreur explicite) :
   - inscription à un voyage **en cours ou à venir** avec statut accepté
     → l'effacement attendra la fin du voyage (obligation légale de
     l'établissement : encadrement, assurance, comptabilité) ;
   - dernier compte `ADMIN` actif ;
   - utilisateur référencé comme `legalGuardian`/parent d'un enfant
     encore actif → à traiter (détacher ou refuser — voir questions ouvertes).
2. **Documents** : collecter les `objectKey`, supprimer les lignes
   `users_documents` + `documents`, puis publier
   `DocumentStorageDeletionEvent(objectKey)` par document (listener existant,
   suppression S3 **après commit** — pattern déjà en place dans
   `DocumentUploadService`). La destruction de la ligne DB détruit la seule
   copie de la DEK wrappée → *crypto-shredding* même si S3 échoue (le
   `deleteObject` best-effort existant suffit).
3. **Health form** : `StudentHealthFormRepository.deleteByStudent(user)` (à ajouter).
4. **Liens familiaux** : `ParentChildRepository.deleteByParentOrChild(user)` (à ajouter).
5. **Voyages** : `trip_user`, `trip_preferences`, `trip_chaperones` —
   suppression explicite (bulk delete) plutôt que de dépendre du cascade JPA
   (le cascade ne joue que si la collection est chargée).
6. **Tokens & credentials** : couverts par les cascades DB de la migration 5.1
   (sinon `deleteByUser` à ajouter sur les 3 repositories).
7. `DELETE users` — puis, hors transaction, log d'audit **sans donnée
   personnelle** (publicId + horodatage uniquement).
8. **`registration_attempt`** : balayer `emailHint` correspondant à l'email
   (best-effort ; déjà purgé par TTL de toute façon).

### 5.3 Controller + sécurité

- `DELETE /api/me` dans `GdprController`.
- **Réauthentification récente exigée** : vérifier `iat`/`auth_time` du JWT
  (< 5 min) ou exiger une confirmation OTP préalable — recommandation :
  réutiliser le flux OTP existant (`purpose=ACCOUNT_DELETION`, nouvelle valeur
  d'enum) : `POST /api/me/delete-request` envoie l'OTP,
  `DELETE /api/me` avec le code en body le consomme. Évite qu'un vol de
  session ≤ 15 min suffise à détruire un compte.
- Réponse `204 No Content` + `Set-Cookie` d'effacement des deux cookies
  (réutiliser `CookieFactory.clear…`).
- Mettre à jour le `DELETE /api/users/{id}` ADMIN existant pour qu'il passe
  par le même `UserErasureService` (aujourd'hui il est cassé — FK violation).

### 5.4 Effacement vs anonymisation

Recommandation : **hard delete intégral** (pas de soft-delete), sauf données
d'inscription aux voyages *passés* si l'établissement a une obligation de
conservation (registres, comptabilité). Deux options :

- **Option A (simple, recommandée en V1)** : tout supprimer, y compris
  l'historique voyages. Les obligations comptables portent sur les factures,
  pas sur `trip_user`.
- **Option B** : anonymiser `trip_user` (user_id → utilisateur sentinelle
  « anonyme », purge de decisionMessage/adminNotes) pour conserver les
  statistiques de participation. Complexité supplémentaire notable.

---

## 6. Chantiers transverses

- **Logs** : `UserService` logue l'email en clair à INFO (lignes ~107, 153, 202, 218) ;
  `RequestAuditFilter` (dev only) logue le username. Remplacer par `publicId`
  dans les logs de prod — sinon l'effacement est incomplet côté logs.
- **Mineurs** : les STUDENT sont des mineurs — l'exercice des droits passe en
  principe par le titulaire de l'autorité parentale. À arbitrer : autoriser
  l'export/l'effacement du compte enfant par le PARENT lié via `parent_child`
  (`GET /api/me/children/{childId}/data-export` ?) ou traiter hors-ligne. **Hors
  périmètre V1 proposé**, mais la structure `parent_child` le permet.
- **OpenAPI/Swagger** : documenter les 3 endpoints (tag « RGPD »).
- **CHANGELOG.md** + section dans `tasks/todo.md` à la livraison.

---

## 7. Fichiers à créer / modifier (récapitulatif)

**Nouveaux :**
- `web/GdprController.java`
- `application/service/GdprService.java` + `impl/GdprServiceImpl.java`
- `application/service/UserErasureService.java` + `impl/UserErasureServiceImpl.java`
- `infrastructure/dto/gdpr/*.java` (records export + `ProfileUpdateRequest`)
- `db/migration/V{n}__gdpr_erasure_support.sql`
- (option) `domain/model/RectificationRequest.java` + repository + migration

**Modifiés :**
- `UserService`/`Impl` — `updateOwnProfile`, refonte `deleteUser` → délègue à `UserErasureService`
- `Section.java` — retirer `cascade=ALL, orphanRemoval=true` sur `users`
- `OtpToken.Purpose` — valeur `ACCOUNT_DELETION`
- Repositories : `StudentHealthFormRepository.deleteByStudent`,
  `ParentChildRepository` (find/deleteByParentOrChild),
  `TripUserRepository`/`TripPreferenceRepository` (deleteByUser),
  `RefreshTokenRepository` (lastUsedAt max)
- `UserController` — dépréciation `POST /api/me`

**Tests (même standard que l'audit) :**
- `GdprServiceImplTest` — complétude de l'export, exclusions (pas de hash, pas d'adminNotes)
- `UserErasureServiceImplTest` — ordre de suppression, garde-fous 409, événements S3 publiés
- `GdprControllerTest` (MockMvc) — 401 sans auth, Content-Disposition, 204 + cookies effacés
- Test de non-régression : `DELETE /api/users/{id}` ADMIN sur user avec documents

---

## 8. Phasage proposé

| Phase | Contenu | Risque | Statut |
|---|---|---|---|
| **1** | Export JSON (accès + portabilité) — lecture seule | Faible | ✅ **Livré 2026-07-02** — `GET /api/me/data-export`, `GdprExportService`, DTOs `infrastructure/dto/gdpr/*`, 12 tests |
| **2** | Rectification (`PATCH /api/me/profile` + demandes identité) | Faible | À faire |
| **3** | Migration FK + `UserErasureService` + `DELETE /api/me` + fix admin delete + fix `Section` cascade | Élevé (destructif) | À faire |

## 9. Arbitrages (décisions du 2026-07-02 — tracées dans [`docs/adr/`](../docs/adr/README.md))

1. **Phase 2** — Demandes de rectification identité : ✅ **table dédiée** (`rectification_request`, traçabilité Art. 12) — [ADR-0002](../docs/adr/0002-rectification-identite-via-table-de-demandes.md)
2. **Phase 3** — Historique voyages : ✅ **suppression totale** (option A) — [ADR-0003](../docs/adr/0003-effacement-par-suppression-totale.md)
3. **Phase 3** — Confirmation d'effacement : ✅ **OTP email** (`purpose=ACCOUNT_DELETION`) — [ADR-0004](../docs/adr/0004-confirmation-de-leffacement-par-otp-email.md)
4. **Phase 3** — Parent référencé par un enfant actif : ✅ **refus 409** (code `guardian_of_active_student`) — [ADR-0006](../docs/adr/0006-refus-409-effacement-dun-tuteur-legal-actif.md)
5. **Droits des mineurs** (§10) : ✅ **matrice par rôle** — [ADR-0005](../docs/adr/0005-droits-rgpd-des-mineurs-matrice-par-role.md)
6. **V2 éventuelle** — Export ZIP incluant les fichiers déchiffrés — *non tranché, non bloquant*.

## 10. Droits des mineurs (STUDENT) — analyse et recommandation

Cadre juridique (à valider par le DPO de l'établissement) :

- Le RGPD ne fixe **aucun âge** pour l'exercice des droits (Art. 15-17) : le
  mineur est titulaire de ses droits. La « majorité numérique » à 15 ans
  (Art. 45 LIL) ne concerne que le **consentement** aux services en ligne,
  pas l'exercice des droits.
- Position CNIL (Recommandation 2) : le mineur peut exercer ses droits
  **directement** quand la démarche constitue un « acte usuel » conforme à son
  intérêt ; les parents peuvent **aussi** les exercer en son nom — les deux
  canaux coexistent.
- Droit à l'effacement renforcé pour les données collectées pendant la
  minorité (Art. 51 LIL, ex-40 ; RGPD 17(1)(f)).
- Contexte scolaire : la base légale n'est pas le consentement (mission
  d'intérêt public / contrat de scolarité) → l'établissement peut refuser un
  effacement tant que la donnée est nécessaire (Art. 17(3)) ; les parents
  restent les interlocuteurs de l'établissement pour un élève mineur.

Matrice retenue (recommandée) :

| Droit | STUDENT (mineur) | Justification |
|---|---|---|
| Accès / portabilité (export) | ✅ direct | Acte usuel, lecture seule, encouragé par la CNIL |
| Rectification (PATCH + demandes) | ✅ direct | Acte usuel, champs contrôlés |
| Effacement (`DELETE /api/me`) | ❌ bloqué (403 + code explicite) | Pas un acte usuel : conséquences lourdes (désinscription voyages) ; passe par le parent ou l'établissement |

Effacement d'un compte STUDENT : à la demande du titulaire de l'autorité
parentale, exécuté par l'ADMIN via le `DELETE /api/users/{id}` existant
(rebranché sur `UserErasureService`). Un endpoint parent-direct
(`DELETE /api/me/children/{childId}` via `parent_child`) est envisageable en V2.
