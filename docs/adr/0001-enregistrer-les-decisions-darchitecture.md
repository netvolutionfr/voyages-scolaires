# ADR-0001 — Enregistrer les décisions d'architecture

- **Statut :** Acceptée
- **Date :** 2026-07-02
- **Décideur :** Stanis

## Contexte

Le projet a accumulé des décisions structurantes (migration ES256, cookies
HttpOnly, audit de sécurité 2026-06-12, plan RGPD) documentées de façon
dispersée (`CHANGELOG.md`, `tasks/*.md`). Les arbitrages — et surtout leurs
justifications — se perdent quand le contexte de la discussion disparaît.

## Décision

Adopter les Architecture Decision Records : un fichier numéroté par décision
dans `docs/adr/`, au format MADR allégé (Statut, Contexte, Décision,
Conséquences), en français. Un ADR accepté n'est plus modifié ; une décision
qui change donne lieu à un nouvel ADR qui remplace l'ancien.

## Conséquences

- Les décisions de conformité RGPD (ADR-0002 à 0006) sont les premières tracées.
- Le `CHANGELOG.md` reste le journal des *changements* ; les ADR tracent les
  *choix* et leurs raisons.
- Les plans d'implémentation restent dans `tasks/` et référencent les ADR.
