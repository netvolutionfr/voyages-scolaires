# API REST pour application Voyages

API Spring Boot **4.1.1**, Java **21**, Gradle **8.14.3** (wrapper fourni).
Authentification basée sur WebAuthn/Passkeys, OTP et des JWT signés en ES256 (ECDSA P-256) côté applicatif.
La clé publique de vérification est exposée sur `GET /.well-known/jwks.json`.

## Présentation

Service REST gérant les participants, sections et entités liées à l'application "Voyages".
Cette API utilise PostgreSQL comme base de données, S3/MinIO pour les fichiers et un serveur WebAuthn embarqué pour l'authentification.

## Hébergement et validation

- Frontend : https://campusaway.fr
- Backend : **https://campusaway.fr/api/**. `api.campusaway.fr` n'est pas le backend de ce projet.
- Environnement de test, sans données réelles ni utilisation réelle pour des voyages.
- Migration déployée ; connexion par passkey confirmée par l'utilisateur le **9 octobre 2026**.
- Validation de la migration : **106 tests réussis**, démarrage PostgreSQL 17 avec huit migrations Flyway et validation Hibernate, santé `UP`, OpenAPI avec 38 chemins ; CI, Trivy et Qodana réussis.

Voir le [changelog](CHANGELOG.md) pour le détail des changements et limites.

## Prérequis

- JDK 21
- Gradle wrapper fourni
- Docker avec Compose pour PostgreSQL et MinIO

## Variables d'environnement

Copier `.env.example` en `.env`, puis renseigner les mots de passe, clés JWT,
clés de chiffrement et accès SMTP. Les valeurs sont des exemples de développement,
à remplacer avant tout usage réel. Ne jamais commiter de secrets.

Spring Boot ne charge pas automatiquement `.env` : exporter les variables dans
le terminal ou utiliser EnvFile dans l'IDE. Compose utilise `.env` et le transmet
au conteneur API.

- `APP_FRONTEND_URL` définit l'URL du frontend.
- `WEBAUTHN_RP_ID` est un nom d'hôte sans schéma ni chemin : `localhost` en local,
  `campusaway.fr` sur le serveur. Les origines incluent le schéma et le port éventuel.
- `JWT_PRIVATE_KEY` : PEM PKCS8 EC P-256 ; `JWT_PUBLIC_KEY` : PEM SPKI.
  Les retours à la ligne peuvent être représentés par des `\n` littéraux.
  Voir [la génération des clés](CLAUDE.md#key-management).
- La configuration lit actuellement `SPRING_JPA_HIBERNATE_DLL_AUTO` (avec `DLL`).
  Utiliser `validate` : Flyway gère le schéma.
- `COOKIE_SECURE=false` est réservé au développement HTTP local ; utiliser `true` en HTTPS.

## Lancer l'application en local

### Avec Gradle (dev)

1. Exporter les variables d'environnement ou utiliser le plugin EnvFile dans IntelliJ.
2. Lancer :

   ```bash
   ./gradlew bootRun --no-daemon --args='--spring.profiles.active=dev'
   ```

   ou définir `SPRING_PROFILES_ACTIVE=dev` et exécuter `./gradlew bootRun`.

### Avec Docker Compose (Postgres + MinIO)

Le projet contient un `docker-compose.yml` pour démarrer Postgres, MinIO (S3) et l'image de référence de l'API. Exemple :

```bash
docker compose up -d db minio minio-init
```

Cette commande démarre uniquement les dépendances. PostgreSQL écoute sur `localhost:5432`, MinIO sur `localhost:9000` et sa console sur `localhost:9001`. Lancer ensuite l'API avec Gradle/IDE.

L'image MinIO épinglée sur Quay a renvoyé une erreur d'accès lors de la migration : un premier démarrage nécessite une image disponible en cache ou un accès au registre rétabli.

### Lancer dans IntelliJ

- Installer le plugin EnvFile (si besoin).
- Dans **Run/Debug Configurations** > **Environment variables**, cocher “EnvFile” et ajouter `.env` à la racine du projet.
- Lancer l'application avec le profile dev : `--spring.profiles.active=dev`.

## Swagger / API docs

Quand l'application tourne, l'OpenAPI / Swagger UI est disponible (si activé) :

```
http://localhost:8080/swagger-ui.html
```

OpenAPI : `http://localhost:8080/v3/api-docs`. Swagger et OpenAPI sont désactivés en profil `prod`.

Pour la compatibilité Jackson 3 avec springdoc 3.1.1, les `JsonNode` sont décrits comme objets JSON et le convertisseur de schémas polymorphiques est désactivé. Réévaluer cette limite lors d'une mise à jour springdoc ou de l'ajout de DTO polymorphiques.

## Tests

Exécuter les tests unitaires et d'intégration :

```bash
./gradlew test
```

Consulter les rapports dans `build/reports/tests`.

## Build

Générer le jar exécutable :

```bash
./gradlew bootJar
```

Le jar sera dans `build/libs/`.

## Points d'attention / Débogage

- Vérifier les variables d'environnement (connexion DB, S3, JWT, WebAuthn).
- Activer les logs `DEBUG` dans `application.properties` si besoin.
- En cas d'erreur d'authentification, contrôler les origins déclarés (`WEBAUTHN_ALLOWED_ORIGINS`) et les clés `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`.
- Le JWKS public est exposé par l'application sur `GET /.well-known/jwks.json` (hors préfixe `/api` ; son accès public dépend du reverse proxy).
- La santé interne est consultable sur `http://127.0.0.1:9090/actuator/health`. Actuator écoute uniquement sur l'interface locale et ne fournit pas les clés JWT.

## Contribuer

- Fork/branch puis PR.
- Respecter les règles de formatage (formatter) et les tests.
- Ajouter des tests pour toute logique métier nouvelle.

## Ressources utiles

- Spring Boot: https://spring.io/projects/spring-boot
- WebAuthn (spec + guides): https://webauthn.guide/
- MinIO / S3 API: https://min.io/docs/
- Swagger / OpenAPI: https://swagger.io/

## Authentification et endpoints principaux

Le navigateur reçoit les cookies HttpOnly `access_token` et `refresh_token`.
Utiliser `credentials: 'include'` avec Fetch ou `withCredentials: true` avec Axios.
L'API accepte aussi `Authorization: Bearer <access_token>`, prioritaire sur le cookie d'accès.

Les chemins sont identiques en local et sur le serveur : par exemple
`https://campusaway.fr/api/me`.

| Méthode et chemin | Usage / accès |
| --- | --- |
| `GET /api/webauthn/authenticate/options` | Options de connexion passkey |
| `POST /api/webauthn/authenticate/finish` | Vérification de la connexion et cookies |
| `POST /api/otp/verify` | Vérification OTP |
| `POST /api/auth/refresh` | Rotation du refresh token et renouvellement des cookies |
| `POST /api/auth/logout` | Révocation et suppression des cookies |
| `GET /api/me` | Profil courant, authentifié |
| `PATCH /api/me/profile` | Modification des coordonnées, authentifié |
| `GET /api/users` | Liste paginée, ADMIN ou TEACHER, filtrée par les règles métier |
| `POST /api/users` | Création d'utilisateur, ADMIN |
| `GET /api/sections` et `GET /api/sections/{id}` | Lecture, authentifié |
| `GET /api/country` | Liste des pays, authentifié |
| `GET /api/me/data-export` | Export RGPD du compte courant |

Les contrôleurs sont dans `src/main/java/fr/siovision/voyages/web` et les DTO dans
`src/main/java/fr/siovision/voyages/infrastructure/dto`.
Voir le [guide d'intégration RGPD](docs/rgpd-frontend-handoff.md).

## Déploiement

Une fusion dans `master` déclenche la publication GHCR puis le déploiement de l'API.
Sur le serveur, la CI exécute :

```bash
docker compose pull api
docker compose up -d --no-deps --wait --wait-timeout 120 api
```

PostgreSQL et MinIO doivent déjà fonctionner. Cette procédure met à jour l'API
sans télécharger à nouveau les images des dépendances ni les redémarrer.

## Documentation

- [Références officielles](HELP.md).
- [Contribution](CONTRIBUTING.md) et [instructions du dépôt](AGENTS.md).
- [Décisions d'architecture](docs/adr/README.md).
- [Historique](CHANGELOG.md).
