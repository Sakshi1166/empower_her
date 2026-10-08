# EmpowerHer

EmpowerHer is a Spring Boot and Thymeleaf portal for browsing government and community schemes.

## Requirements

- Java 21
- Maven Wrapper (`mvnw.cmd` on Windows or `./mvnw` on Linux/macOS)
- MySQL 8-compatible database

## Local setup

1. Copy `.env.example` to `.env` or export the variables in your shell.
2. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the local MySQL instance.
3. Start the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Open `http://localhost:8081`.

The application does not create predictable default users by default. If development seed accounts are needed, set `BOOTSTRAP_CREATE_DEFAULT_USERS=true` and provide all four `BOOTSTRAP_*` username/password variables. Never use these seed values in a public deployment.

## Configuration and secrets

Runtime configuration is supplied through environment variables. The repository intentionally contains no database password, SMTP password, API token, or production account credentials.

Important variables:

| Variable | Purpose |
| --- | --- |
| `PORT` | HTTP port supplied by the hosting platform |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `APP_PUBLIC_URL` | Public HTTPS URL used in email links |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP credentials |
| `FILE_UPLOAD_DIR` | Upload directory |
| `JPA_DDL_AUTO` | Schema behavior; use `validate` in production |

Keep `.env` files, logs, and runtime uploads out of Git. Rotate any credential that has ever been committed or shared.

## Tests

```powershell
.\mvnw.cmd test
```

The current integration test starts the full application and therefore requires a reachable MySQL database unless a test datasource is supplied.

## Deployment

See the complete [deployment guide](./DEPLOYMENT.md) for:

- GitHub-safe pre-push checks
- External MySQL provisioning
- Render build/start settings
- Private environment variables
- First schema initialization
- HTTPS and authentication smoke tests
- Free-tier upload limitations
- Troubleshooting and rollback guidance

The repository includes [render.yaml](./render.yaml) and a multi-stage [Dockerfile](./Dockerfile) for a Docker-based Render web service. Configure database and SMTP values as private environment variables in the hosting dashboard; never put their values in `render.yaml` or GitHub.
