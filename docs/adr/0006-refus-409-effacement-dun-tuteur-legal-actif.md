# ADR-0006 — RGPD : refus 409 de l'effacement d'un tuteur légal référencé par un enfant actif

- **Statut :** Acceptée
- **Date :** 2026-07-02
- **Décideur :** Stanis
- **Contexte projet :** [`tasks/rgpd-plan.md`](../../tasks/rgpd-plan.md) §5.2

## Contexte

Un utilisateur PARENT peut être référencé comme tuteur légal
(`users.legal_guardian_user_id`) ou lié via `parent_child` à un élève encore
actif. Son effacement laisserait l'établissement sans responsable légal
joignable pour un mineur inscrit — inacceptable opérationnellement (urgences
en voyage, autorisations). Deux options : refuser l'effacement tant que le
lien existe, ou détacher silencieusement (`SET NULL`) et effacer quand même.

## Décision

**Refus HTTP 409** avec un code d'erreur explicite (ex.
`guardian_of_active_student`) tant que l'utilisateur est tuteur légal ou
parent lié d'un élève actif. L'effacement redevient possible quand le lien
est levé (autre tuteur désigné par l'établissement, ou élève parti/effacé).

Ce refus s'appuie sur l'Art. 17(3) RGPD : les données du tuteur restent
nécessaires à la mission de l'établissement vis-à-vis du mineur.

## Conséquences

- Garde-fou vérifié en tête de `UserErasureService`, avec les deux autres
  (voyage en cours, dernier ADMIN) — même pattern de réponse 409.
- Le front devra afficher un message expliquant la marche à suivre (étape
  ultérieure).
- Alternative rejetée : détachement silencieux — créerait des élèves mineurs
  sans responsable légal en base, et masquerait la cause à l'utilisateur.
- La migration prévue (`ON DELETE SET NULL` sur `legal_guardian_user_id`)
  reste utile comme filet technique, mais le service ne doit jamais l'atteindre
  pour un enfant actif.
