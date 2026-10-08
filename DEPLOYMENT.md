# EmpowerHer deployment guide

This guide publishes EmpowerHer to GitHub and deploys it as a Render-style Java web service connected to an external MySQL-compatible database.

The guide never requires putting a password, SMTP app password, or token in GitHub or in this repository. Set secrets only in your local environment or in the hosting provider's private environment-variable settings.

## 1. Prepare the project locally

Open PowerShell and move to the project directory:

```powershell
Set-Location 'C:\Users\<your-user>\path\to\empower-her'
```

Confirm the project builds and tests:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package -DskipTests
```

The test profile uses H2, so a local MySQL server is not required for the test command.

Create a local environment file from the safe template:

```powershell
Copy-Item .env.example .env
```

Edit `.env` with local-only values. `.env` is ignored by Git and must never be committed.

For local development, the important values are:

```text
DB_URL=jdbc:mysql://localhost:3307/empowerher_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=<local-password>
APP_PUBLIC_URL=http://localhost:8081
```

If you need local seed accounts, set all of these temporarily:

```text
BOOTSTRAP_CREATE_DEFAULT_USERS=true
BOOTSTRAP_ADMIN_USERNAME=<temporary-admin-name>
BOOTSTRAP_ADMIN_PASSWORD=<strong-temporary-password>
BOOTSTRAP_USER_USERNAME=<temporary-user-name>
BOOTSTRAP_USER_PASSWORD=<strong-temporary-password>
```

Never use seed credentials in a public deployment.

## 2. Check that no secret or runtime file will be pushed

Run:

```powershell
git status --short
git check-ignore -v .env logs uploads target
git ls-files | Select-String -Pattern '(^|/)(\.env|logs|uploads|target)/|application-local\.properties'
```

The last command should not report local secrets or runtime directories.

Run a basic text scan before staging:

```powershell
Get-ChildItem -Recurse -File -Force |
  Where-Object { $_.FullName -notmatch '\\(target|uploads|logs|\.git)\\' } |
  Select-String -Pattern 'admin123|user123|BEGIN (RSA|OPENSSH|PRIVATE) KEY|xeuj|password\s*=\s*[^$<\s]' -CaseSensitive:$false
```

Review every match manually. Environment placeholders such as `${DB_PASSWORD:}` are safe; real values are not.

If a real credential was ever committed, rotate it first. Removing the file from the latest commit does not make an old Git commit safe.

## 3. Create the GitHub repository

Create an empty repository on GitHub. Do not add a second README, `.gitignore`, or license during creation.

From the project directory:

```powershell
git status
git add .gitignore .env.example README.md DEPLOYMENT.md render.yaml pom.xml src
git diff --cached --check
git diff --cached --stat
```

Inspect the staged file list. Do not continue if `.env`, logs, uploads, `target`, private keys, or credentials are present:

```powershell
git diff --cached --name-only
```

Commit and push:

```powershell
git commit -m "Prepare EmpowerHer for safe deployment"
git branch -M main
git remote add origin https://github.com/<github-user>/<repository>.git
git push -u origin main
```

If `origin` already exists, inspect it instead of adding it again:

```powershell
git remote -v
```

After pushing, use GitHub's secret-scanning/security view and inspect the repository tree. Do not upload `.env` through the GitHub website.

## 4. Provision the external MySQL database

Choose a currently available free MySQL-compatible provider that allows connections from an external web service. Free plans change frequently, so verify the provider's current limits, expiry rules, SSL requirements, and connection policy before creating the database.

Create:

1. A database.
2. A dedicated application database user.
3. A long random password.
4. A connection endpoint accessible by the Render service.

Record these values privately:

```text
DB host
DB port
DB name
DB username
DB password
SSL requirements
```

Build the JDBC URL from the provider's values. Do not paste the password into the URL if the provider gives a separate username/password field:

```text
jdbc:mysql://<host>:<port>/<database>?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

Test the connection locally only if the provider permits it. Never commit the resulting connection string.

### Initialize the schema

For the first deployment only, configure the service temporarily with:

```text
JPA_DDL_AUTO=update
```

Start the service once so Hibernate creates the tables. Inspect the logs for successful datasource initialization. After the first successful startup, change the service value to:

```text
JPA_DDL_AUTO=validate
```

This prevents the application from silently changing the production schema on later deployments.

For future schema changes, use a migration tool such as Flyway or Liquibase rather than leaving production on `update`.

## 5. Create the Render web service

1. Sign in to Render.
2. Create a new Web Service.
3. Connect the GitHub repository.
4. Select the `main` branch.
5. Use the Java runtime and the repository's `render.yaml` values, or enter these manually:

```text
Build command: ./mvnw clean package -DskipTests
Start command: java -Dserver.port=$PORT -jar target/empowerher-0.0.1-SNAPSHOT.jar
```

The application reads the platform-provided `PORT`. Do not hardcode a public hosting port.

## 6. Configure private Render environment variables

Add these in the Render dashboard. Do not put their values in `render.yaml` or GitHub:

| Variable | Initial value |
| --- | --- |
| `DB_URL` | JDBC URL for the external MySQL database |
| `DB_USERNAME` | Dedicated application database username |
| `DB_PASSWORD` | Dedicated application database password |
| `APP_PUBLIC_URL` | Render HTTPS URL, for example `https://your-service.onrender.com` |
| `SESSION_COOKIE_SECURE` | `true` |
| `JPA_DDL_AUTO` | `update` only for first schema initialization, then `validate` |
| `THYMELEAF_CACHE` | `true` |
| `DEVTOOLS_RESTART_ENABLED` | `false` |
| `DEVTOOLS_LIVERELOAD_ENABLED` | `false` |
| `BOOTSTRAP_CREATE_DEFAULT_USERS` | `false` |

Optional email variables:

| Variable | Value |
| --- | --- |
| `MAIL_USERNAME` | Dedicated SMTP account |
| `MAIL_PASSWORD` | SMTP app password or provider token |
| `MAIL_FROM` | Verified sender address |
| `MAIL_ADMIN` | Admin notification address |
| `MAIL_SUPPORT` | Support address |

If email is not configured, disable or avoid flows that require email until SMTP is ready. Never use a personal Gmail password; use an app password or a transactional email provider credential.

## 7. Deploy and initialize

1. Trigger the first deployment.
2. Watch the build log for a successful Maven package.
3. Watch the runtime log for datasource and JPA startup.
4. Open the generated HTTPS URL.
5. If the application starts successfully, confirm the database tables exist.
6. Change `JPA_DDL_AUTO` from `update` to `validate`.
7. Redeploy.
8. Set `APP_PUBLIC_URL` to the final HTTPS URL if the URL changed, then redeploy once more.

Do not expose or paste complete deployment logs if they contain connection URLs, usernames, email addresses, or provider metadata.

## 8. First-deployment smoke test

Run these checks in a private browser window:

1. `/` and `/home` load over HTTPS.
2. Public scheme and category pages load.
3. Registration creates a user.
4. User login works.
5. `POST /logout` works from the UI.
6. A normal user cannot open `/admin/dashboard`.
7. An admin can open the admin dashboard.
8. Admin scheme/category operations work.
9. Comments and share operations work only through the intended authenticated flow.
10. Database changes remain after a service restart/redeploy.
11. Email links, if enabled, point to the HTTPS `APP_PUBLIC_URL`.
12. Browser developer tools and GitHub do not show database or SMTP credentials.

Check the expected authorization behavior with PowerShell:

```powershell
Invoke-WebRequest 'https://your-service.onrender.com/home'
try { Invoke-WebRequest 'https://your-service.onrender.com/admin/dashboard' } catch { $_.Exception.Response.StatusCode }
```

The admin request should not be publicly accessible.

## 9. Uploads and free hosting

The current application writes uploads to `FILE_UPLOAD_DIR`, which defaults to a local `uploads/` directory. Many free web services use ephemeral filesystems. Files may disappear after a restart, redeploy, or instance replacement.

Before treating uploads as production data:

- Add external object storage or another durable file service.
- Store only generated filenames and metadata in MySQL.
- Restrict upload MIME types and size.
- Back up existing files before redeploying.
- Test an upload, redeploy, and download the file again.

Until durable storage is configured, describe uploads as temporary in the public deployment.

## 10. Troubleshooting

### Application fails to start with a datasource error

- Confirm `DB_URL` uses the provider's external hostname, not `localhost`.
- Confirm the database allows external connections.
- Confirm the database user, password, port, and SSL parameters.
- Confirm the provider has not suspended the free database.

### Application binds successfully but the service is unreachable

- Confirm the start command uses `$PORT`.
- Confirm `server.port=${PORT:8081}` is present.
- Check the Render runtime log for a port-binding error.

### Login redirects or cookies fail

- Confirm the service is accessed over HTTPS.
- Confirm `SESSION_COOKIE_SECURE=true`.
- Confirm the login/logout forms include the existing CSRF token.

### Email fails

- Confirm SMTP username and app password are set as private variables.
- Confirm the sender address is accepted by the provider.
- Temporarily leave email unconfigured while validating the core application.

### Uploaded files disappear

This is expected on an ephemeral filesystem. Configure external durable storage; do not solve it by committing uploads to GitHub.

## 11. Ongoing maintenance

- Enable GitHub Dependabot/security alerts and branch protection.
- Rotate database and SMTP credentials if they may have been exposed.
- Keep production on `JPA_DDL_AUTO=validate`.
- Back up MySQL data and durable uploads.
- Review free-tier sleep, bandwidth, storage, connection, and retention limits.
- Keep a rollback commit and document how to redeploy it.
