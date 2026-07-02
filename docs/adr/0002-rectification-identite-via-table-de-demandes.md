# ADR-0002 — RGPD : rectification des champs d'identité via une table de demandes

- **Statut :** Acceptée
- **Date :** 2026-07-02
- **Décideur :** Stanis
- **Contexte projet :** [`tasks/rgpd-plan.md`](../../tasks/rgpd-plan.md) §4

## Contexte

Le droit de rectification (Art. 16 RGPD) couvre toutes les données inexactes.
Or les champs d'état civil (`firstName`, `lastName`, `birthDate`) relèvent du
dossier scolaire contrôlé par l'établissement, et `email` est l'identifiant
d'authentification (unicité, dérivation du `userHandle` WebAuthn). Une
modification self-service directe de ces champs est exclue. Deux options pour
exercer le droit malgré tout : un traitement manuel hors application (mailto
au DPO), ou une demande structurée en base.

## Décision

Les demandes de rectification des champs d'identité passent par une **table
dédiée** `rectification_request` (champ visé, valeur souhaitée, motif,
horodatage, statut), alimentée par `POST /api/me/rectification-request` et
traitée par un ADMIN via le `PUT /api/users/{id}` existant.

Les champs de contact (`telephone`, `displayName`, `gender`) restent
modifiables directement via `PATCH /api/me/profile`.

## Conséquences

- Traçabilité native du délai de réponse (Art. 12 RGPD : réponse sous un mois)
  — l'horodatage de la demande fait foi.
- Une migration Flyway, une entité, un repository et un écran admin (front,
  étape ultérieure) supplémentaires.
- Alternative rejetée : traitement manuel hors app — pas de traçabilité,
  charge de suivi reportée sur le DPO.
