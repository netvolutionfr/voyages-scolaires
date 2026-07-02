# ADR-0003 — RGPD : effacement par suppression totale (pas d'anonymisation)

- **Statut :** Acceptée
- **Date :** 2026-07-02
- **Décideur :** Stanis
- **Contexte projet :** [`tasks/rgpd-plan.md`](../../tasks/rgpd-plan.md) §5.4

## Contexte

Lors de l'effacement d'un compte (Art. 17 RGPD), deux stratégies étaient
possibles pour l'historique des inscriptions aux voyages (`trip_user`,
`trip_preferences`) :

- **Option A** — suppression totale, y compris l'historique voyages ;
- **Option B** — anonymisation (rattachement à un utilisateur sentinelle
  « anonyme », purge des champs libres) pour conserver des statistiques de
  participation.

Les obligations de conservation de l'établissement portent sur les pièces
comptables (factures), pas sur les lignes d'inscription applicatives.

## Décision

**Option A : suppression totale.** L'effacement d'un compte supprime toutes
les données liées : documents (lignes DB + objets S3 via crypto-shredding de
la DEK), fiche santé, liens familiaux, inscriptions et préférences voyages,
tokens et credentials, puis la ligne `users`. Aucun soft-delete, aucun
utilisateur sentinelle.

Garde-fous préalables (refus HTTP 409) : inscription acceptée à un voyage en
cours ou à venir ; dernier compte ADMIN actif ; tuteur légal référencé par un
enfant actif (voir ADR-0006).

## Conséquences

- Implémentation plus simple et conformité maximale : aucune donnée résiduelle.
- Perte assumée des statistiques de participation historiques.
- Le `DELETE /api/users/{id}` ADMIN existant doit être rebranché sur le même
  `UserErasureService` (il provoque aujourd'hui une violation FK).
- Les logs applicatifs ne doivent plus contenir d'email en clair, sinon
  l'effacement est incomplet (chantier transverse du plan, §6).
