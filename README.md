# API REST pour application Voyages

API écrite avec le framework Spring Boot.
Authentification par JWT, fourni par Keycloak

## Développement local

L’API utilise des variables d’environnement (stockées dans `.env`) pour se connecter
à PostgreSQL et Keycloak.

### IntelliJ IDEA
- Installer le plugin **EnvFile** (si besoin).
- Dans **Run/Debug Configurations** > **Environment variables**, cocher “EnvFile” et ajouter `.env` à la racine du projet.
- Lancer l’application avec `--spring.profiles.active=dev`.

### Autres IDE
Exporter les variables dans le shell avant de lancer :
```bash
export $(grep -v '^#' .env | xargs)
./gradlew bootRun
