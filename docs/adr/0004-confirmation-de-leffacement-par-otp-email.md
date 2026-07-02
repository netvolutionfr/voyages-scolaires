# ADR-0004 — RGPD : confirmation de l'effacement de compte par OTP email

- **Statut :** Acceptée
- **Date :** 2026-07-02
- **Décideur :** Stanis
- **Contexte projet :** [`tasks/rgpd-plan.md`](../../tasks/rgpd-plan.md) §5.3

## Contexte

`DELETE /api/me` est l'action la plus destructrice de l'application
(irréversible : crypto-shredding des documents, suppression de la fiche
santé). Un JWT d'accès reste valable 15 minutes : un vol de session — cookie
exfiltré, poste partagé dans un établissement scolaire — suffirait à détruire
un compte. Deux mécanismes de réauthentification étaient envisagés : exiger
un JWT « frais » (`iat` < 5 min), ou exiger une confirmation par OTP email.

## Décision

**Confirmation par OTP email**, en réutilisant le flux OTP existant avec une
nouvelle valeur d'enum `OtpToken.Purpose.ACCOUNT_DELETION` :

1. `POST /api/me/delete-request` — envoie l'OTP à l'email du compte ;
2. `DELETE /api/me` avec le code en body — vérifie et consomme l'OTP, puis
   déclenche `UserErasureService`.

## Conséquences

- Preuve de possession de la boîte email, indépendante de la session : un
  vol de cookie ne suffit plus.
- Réutilise la machinerie durcie lors de l'audit 2026-06-12 (BCrypt, verrou
  pessimiste, cooldown + quota glissant) — pas de nouveau code sensible.
- Alternative rejetée : fraîcheur du JWT — ne protège pas contre un vol de
  session récent et complique le front (re-login forcé).
- Le front devra enchaîner les deux appels (étape ultérieure).
