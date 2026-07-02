# RGPD — Endpoints backend disponibles (à intégrer côté front)

Base URL inchangée. Tous les endpoints ci-dessous nécessitent une session authentifiée (cookie `access_token`/`refresh_token` existant). Aucun changement sur le flux d'auth.

Contexte complet : [`tasks/rgpd-plan.md`](../tasks/rgpd-plan.md) et [`docs/adr/`](adr/README.md) (ADR-0002 à 0006).

## 1. Export de mes données — `GET /api/me/data-export`

- **Auth** : n'importe quel rôle authentifié.
- **Réponse** : `200`, `Content-Type: application/json`, avec un header `Content-Disposition: attachment; filename="voyages-export-{publicId}.json"` — le navigateur proposera un téléchargement si vous laissez le comportement par défaut du `fetch`/lien. Si vous voulez l'afficher au lieu de le télécharger, il faudra ignorer ce header côté front (ne pas naviguer directement dessus, faire un `fetch` + traiter le JSON).
- **Corps de la réponse** :

```jsonc
{
  "exportedAt": "2026-07-02T10:00:00Z",
  "format": "voyages-gdpr-export/v1",
  "profile": {
    "publicId": "uuid",
    "email": "string",
    "firstName": "string", "lastName": "string", "displayName": "string",
    "gender": "M|F|N|null",
    "birthDate": "2010-05-01|null",       // string ISO, pas un objet date
    "telephone": "string|null",
    "role": "ADMIN|PARENT|STUDENT|TEACHER",
    "status": "ACTIVE|INACTIVE|PENDING|BANNED",
    "section": "string|null",              // label de la section, pas un objet
    "createdAt": "2025-09-01T08:00:00",    // LocalDateTime, PAS de suffixe Z
    "updatedAt": "2026-01-01T08:00:00"
  },
  "consent": { "givenAt": "2026-01-01|null", "text": "string|null" },
  "legalGuardian": { "publicId": "uuid", "fullName": "string", "email": "string" } /* ou null */,
  "familyLinks": [
    { "relation": "CHILD|PARENT", "publicId": "uuid", "fullName": "string", "email": "string" }
  ],
  "trips": {
    "registrations": [
      { "tripTitle": "string", "destination": "string|null",
        "departureDate": "2026-04-08|null", "returnDate": "2026-04-15|null",
        "registrationStatus": "PENDING|VALIDATED|REJECTED|ENROLLED|CONFIRMED|CANCELED",
        "registrationDate": "2026-01-05T10:00:00|null", "decisionDate": "…|null",
        "decisionMessage": "string|null" }
      // note : PAS de champ adminNotes (volontairement exclu, note interne établissement)
    ],
    "preferences": [ { "tripTitle": "string", "interest": "YES|NO" } ]
  },
  "documents": [
    { "type": "string|null",              // libellé du DocumentType, pas son code
      "originalFilename": "string|null", "mime": "string|null", "size": 12345,
      "sha256": "string|null", "fileNumber": "string|null",
      "deliveryDate": "…|null", "expirationDate": "…|null", "createdAt": "…" }
    // les fichiers eux-mêmes ne sont PAS inclus, seulement les métadonnées ;
    // pour télécharger un fichier : GET /api/documents/{docId}/preview (existant, inchangé)
  ],
  "healthForm": { "signedAt": "2026-01-10T00:00:00Z", "validUntil": "…", "payload": { /* JSON libre déchiffré */ } } /* ou null si aucune fiche */,
  "security": {
    "passkeys": [ { "createdAt": "…", "aaguid": "string" } ],
    "lastLoginAt": "2026-06-30T12:00:00Z|null"
  }
}
```

**Point d'attention front** : `familyLinks` et `legalGuardian` exposent le nom et l'email d'un tiers (le parent ou l'enfant lié), pas seulement de l'utilisateur qui exporte. Prévoir un texte explicatif si affiché à l'écran.

---

## 2. Rectification — self-service (contact)

### `PATCH /api/me/profile`

- **Body** (tous les champs optionnels, sémantique PATCH — absent = inchangé) :
```json
{ "telephone": "string", "displayName": "string", "gender": "M|F|N" }
```
- **Réponse** : `200` avec le même objet que `GET /api/me` (`UserResponse` : `publicId, email, lastName, firstName, fullName, gender, birthDate, telephone, section, sectionPublicId, status, role`).
- **Erreurs** : `400 {"error":"Invalid request"}` si `gender` n'est pas `M`/`F`/`N`.
- ⚠️ **`POST /api/me` (ancien endpoint de mise à jour du téléphone) est marqué `@Deprecated` côté backend mais reste fonctionnel.** Migrer progressivement vers `PATCH /api/me/profile`, pas de deadline de suppression fixée pour l'instant.

## 3. Rectification — champs d'identité (via demande)

Les champs `firstName`, `lastName`, `birthDate`, `email` ne sont **pas** modifiables directement (dossier scolaire / identifiant d'authentification). Il faut passer par une demande.

### `POST /api/me/rectification-request`

- **Body** :
```json
{ "field": "FIRST_NAME|LAST_NAME|BIRTH_DATE|EMAIL", "requestedValue": "string", "reason": "string (optionnel)" }
```
- **Réponse `201`** :
```json
{ "id": "uuid", "field": "LAST_NAME", "requestedValue": "Martin", "reason": "Mariage",
  "status": "PENDING", "createdAt": "2026-07-02T…Z", "processedAt": null, "adminComment": null }
```
- **Erreurs `400`** : `field` invalide, `requestedValue` vide, format email invalide (doit contenir `@`), `birthDate` non parseable en `yyyy-MM-dd`.
- **Important** : la demande ne modifie rien immédiatement. Le front doit afficher un message du type *"Votre demande a été transmise, elle sera traitée sous 1 mois"* — pas de confirmation de changement effectif.

### Écrans ADMIN (à construire si besoin d'une UI de traitement)

- `GET /api/users/rectification-requests?status=PENDING&page=0&size=20` — `hasRole('ADMIN')`. Renvoie une `Page` Spring standard (`content`, `totalElements`, `totalPages`, etc.) de :
```json
{ "id": "uuid", "userPublicId": "uuid", "userFullName": "string", "userEmail": "string",
  "field": "LAST_NAME", "requestedValue": "Martin", "reason": "string|null",
  "status": "PENDING", "createdAt": "…" }
```
- `PATCH /api/users/rectification-requests/{id}` — `hasRole('ADMIN')`, body `{ "status": "APPLIED|REJECTED", "adminComment": "string (optionnel)" }`. **Ne modifie pas le champ lui-même** — l'admin doit d'abord appliquer le changement via le `PUT /api/users/{userPublicId}` existant, puis clôturer la demande avec ce PATCH. Deux actions distinctes à enchaîner dans l'UI admin.

---

## 4. Effacement du compte — self-service

Flux en deux temps (confirmation par email, ADR-0004).

### Étape 1 — `POST /api/me/delete-request`
- Pas de body.
- Réponse `200 { "message": "A confirmation code has been sent to your email address." }`
- Déclenche l'envoi d'un email avec un code à 6 chiffres (même gabarit que l'OTP de connexion existant, TTL ~10 min).

### Étape 2 — `DELETE /api/me`
- **Body** : `{ "otp": "123456" }`
- **Succès** : `204 No Content` + `Set-Cookie` qui efface `refresh_token`. **Le compte et toutes ses données sont supprimés de façon irréversible** (documents, fiche santé, inscriptions voyages, liens familiaux, tokens). Rediriger immédiatement vers l'écran de déconnexion/accueil — la session est morte même si l'access token en mémoire a encore quelques minutes de validité théorique.
- **Erreurs à gérer explicitement dans l'UI** :

| Statut | Corps `error` | Signification | Suggestion UI |
|---|---|---|---|
| `400` | `invalid_otp` | Code manquant, faux, expiré, ou déjà utilisé | Réafficher le champ code avec message d'erreur, permettre de redemander |
| `429` | `too_many_requests` | Trop de demandes de code rapprochées (`POST /delete-request` spammé) | "Veuillez patienter avant de redemander un code" |
| `403` | `student_self_erasure_forbidden` | Le compte est un STUDENT (mineur) — **le bouton de suppression ne devrait même pas être accessible pour ce rôle** | Masquer l'action pour les comptes STUDENT ; message "Contactez l'établissement ou votre parent" si atteint quand même |
| `409` | `last_active_admin` | Dernier compte ADMIN actif du système | "Impossible : vous êtes le dernier administrateur actif" |
| `409` | `active_trip_registration` | Inscription validée/confirmée à un voyage en cours ou à venir | "Vous avez une inscription active à un voyage, attendez son terme" |
| `409` | `guardian_of_active_student` | L'utilisateur est tuteur légal ou parent lié d'un élève encore actif | "Vous êtes le contact responsable d'un élève actif, ce lien doit être levé avant" |

Recommandation : traiter `403`/`409` par un `error.error` générique mappé sur un dictionnaire de messages front (le code est stable, le texte peut être localisé/affiné librement côté front).

---

## Résumé des routes

| Méthode | Route | Rôle | Statut |
|---|---|---|---|
| GET | `/api/me/data-export` | tout rôle | nouveau |
| PATCH | `/api/me/profile` | tout rôle | nouveau |
| POST | `/api/me` | tout rôle | **déprécié**, garder en fallback |
| POST | `/api/me/rectification-request` | tout rôle | nouveau |
| GET | `/api/users/rectification-requests` | ADMIN | nouveau |
| PATCH | `/api/users/rectification-requests/{id}` | ADMIN | nouveau |
| POST | `/api/me/delete-request` | tout rôle | nouveau |
| DELETE | `/api/me` | tout rôle (bloqué en pratique pour STUDENT) | nouveau |
