# API REST pour application Voyages

API écrite avec le framework Spring Boot.
Authentification par JWT, fourni par Keycloak.

## Présentation

Service REST gérant les participants, sections et entités liées à l'application "Voyages".
Cette API utilise PostgreSQL comme base de données et Keycloak pour l'authentification/autorisation.

## Prérequis

- JDK 17 (ou version configurée dans le projet)
- Gradle wrapper (fourni) ou Gradle installé
- Docker & docker-compose (optionnel pour exécuter Keycloak/Postgres localement)
- Node/npm (si vous utilisez des outils front ou scripts complémentaires)

## Variables d'environnement

L'application lit des variables d'environnement (fichier `.env` recommandé) utilisées pour se connecter à la base de données et Keycloak.
Voici un exemple minimal (.env) :

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/voyages
SPRING_DATASOURCE_USERNAME=voyages
SPRING_DATASOURCE_PASSWORD=changeit
SPRING_JPA_HIBERNATE_DDL_AUTO=update

KEYCLOAK_SERVER_URL=http://localhost:8080/auth
KEYCLOAK_REALM=voyages-realm
KEYCLOAK_RESOURCE=voyages-client
KEYCLOAK_CREDENTIALS_SECRET=your-client-secret

# Optionnel pour le profile dev
SPRING_PROFILES_ACTIVE=dev
```

Ne placez pas de secrets en clair dans le dépôt. Utilisez un mécanisme sécurisé pour CI/CD.

## Lancer l'application en local

### Avec Gradle (dev)

1. Exporter les variables d'environnement ou utiliser le plugin EnvFile dans IntelliJ.
2. Lancer :

   ```bash
   ./gradlew bootRun --no-daemon --args='--spring.profiles.active=dev'
   ```

   ou définir `SPRING_PROFILES_ACTIVE=dev` et exécuter `./gradlew bootRun`.

### Avec Docker Compose (Keycloak + Postgres)

Le projet contient un `docker-compose.yml` pour démarrer Postgres et Keycloak en local. Exemple :

```bash
docker-compose up -d
```

Ensuite lancer l'application (gradle ou depuis l'IDE).

### Lancer dans IntelliJ

- Installer le plugin EnvFile (si besoin).
- Dans **Run/Debug Configurations** > **Environment variables**, cocher “EnvFile” et ajouter `.env` à la racine du projet.
- Lancer l'application avec le profile dev : `--spring.profiles.active=dev`.

## Swagger / API docs

Quand l'application tourne, l'OpenAPI / Swagger UI est disponible (si activé) :

```
http://localhost:8080/swagger-ui.html
```

(adapter l'URL si vous avez changé le port ou le contexte dans application.properties)

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

- Vérifier les variables d'environnement (connexion DB, Keycloak).
- Activer les logs `DEBUG` dans `application.properties` si besoin.
- En cas d'erreur d'authentification, vérifier les réglages du client Keycloak (redirects, secret, realm).

## Contribuer

- Fork/branch puis PR.
- Respecter les règles de formatage (formatter) et les tests.
- Ajouter des tests pour toute logique métier nouvelle.

## Ressources utiles

- Keycloak: https://www.keycloak.org/
- Spring Boot: https://spring.io/projects/spring-boot
- Swagger / OpenAPI: https://swagger.io/

## Endpoints principaux

Ci‑dessous une documentation concise des endpoints principaux exposés par l'API.

Authentification
- Toutes les requêtes protégées nécessitent un header `Authorization: Bearer <access_token>` fourni par Keycloak.

Participants

- POST /api/participants
  - Description : créer un participant.
  - Autorisation : PARENT ou ADMIN.
  - Body (JSON) :
    {
      "prenom": "Jean",
      "nom": "Dupont",
      "dateNaissance": "2010-05-20",
      "email": "jean.dupont@example.com",
      "sexe": "M",
      "telephone": "+33123456789",
      "sectionId": 1,
      "legalGuardianId": "<uuid>" ,
      "createStudentAccount": false
    }
  - Réponse : ParticipantProfileResponse (201/200 selon implémentation).
  - Exemple curl :
    curl -X POST http://localhost:8080/api/participants \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"prenom":"Jean","nom":"Dupont","dateNaissance":"2010-05-20","sectionId":1}'

- GET /api/participants
  - Description : récupérer une liste paginée de participants.
  - Paramètres : q (recherche texte), page, size, sort
  - Autorisation : ADMIN ou PARENT.
  - Exemple : GET /api/participants?q=dupont&page=0&size=20

- GET /api/participants/{id}
  - Description : récupérer le profil d'un participant par UUID.
  - Autorisation : ADMIN ou parent légal.

- GET /api/participants?email={email}
  - Description : récupérer un participant par email (si exposé par le controller).
  - Autorisation : ADMIN ou PARENT.

- PUT /api/participants/{id}
  - Description : mettre à jour un participant.
  - Autorisation : ADMIN (ou PARENT selon règles métier).

- DELETE /api/participants/{id}
  - Description : supprimer un participant.
  - Autorisation : ADMIN.

Sections

- GET /api/sections
  - Description : lister / rechercher des sections.
  - Paramètres : q, page, size, sort
  - Autorisation : ouverte en lecture selon configuration (souvent publique ou restreinte).
  - Exemple curl :
    curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/sections?q=rand&page=0&size=10"

- GET /api/sections/{id}
  - Description : récupérer une section par son id.

Notes pratiques
- Validez le schéma JSON des DTOs dans `src/main/java/.../infrastructure/dto` pour connaître les champs exacts et les contraintes.
- Les codes HTTP usuels : 200 (OK), 201 (Created), 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 409 (Conflict).
