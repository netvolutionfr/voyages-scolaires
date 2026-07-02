# ADR-0005 — RGPD : droits des mineurs — matrice d'exercice par rôle

- **Statut :** Acceptée
- **Date :** 2026-07-02
- **Décideur :** Stanis
- **Contexte projet :** [`tasks/rgpd-plan.md`](../../tasks/rgpd-plan.md) §10

## Contexte

Les comptes STUDENT appartiennent à des mineurs. Le RGPD ne fixe aucun âge
pour l'exercice des droits — le mineur en est titulaire. La CNIL
([Recommandation 2](https://www.cnil.fr/fr/recommandation-2-encourager-les-mineurs-exercer-leurs-droits))
admet l'exercice direct par le mineur quand la démarche constitue un
« acte usuel » conforme à son intérêt, les parents pouvant toujours agir en
son nom. La « majorité numérique » à 15 ans (Art. 45 LIL) ne concerne que le
consentement, pas l'exercice des droits. En contexte scolaire, la base légale
n'est pas le consentement : l'établissement peut refuser un effacement tant
que les données sont nécessaires (Art. 17(3)).

## Décision

Discrimination **par rôle** (pas de calcul d'âge sur `birthDate` chiffré) :

| Droit | STUDENT | Justification |
|---|---|---|
| Accès / portabilité (`GET /api/me/data-export`) | ✅ direct | Acte usuel, lecture seule |
| Rectification (`PATCH /api/me/profile` + demandes) | ✅ direct | Acte usuel, champs contrôlés |
| Effacement (`DELETE /api/me`) | ❌ 403 + code explicite | Pas un acte usuel (désinscription voyages, destruction fiche santé) |

L'effacement d'un compte STUDENT s'exerce par le titulaire de l'autorité
parentale auprès de l'établissement, exécuté par un ADMIN via
`DELETE /api/users/{id}` (rebranché sur `UserErasureService`).

## Conséquences

- Aucune logique d'âge à implémenter ni à maintenir.
- Lecture prudente et défendable de la doctrine CNIL ; à faire valider par le
  DPO de l'établissement.
- Un endpoint parent-direct (`DELETE /api/me/children/{childId}` via
  `parent_child`) reste possible en V2 sans remettre en cause cette décision.
