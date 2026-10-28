# UP SELF

UP-SELF is a Java and Vue learning platform for a small student group in Malaysia. It includes course allocation and teaching content, private media, assignments and feedback, weekly mood, learning progress, private goals, bookmarks and general self-efficacy assessments.

Read the [Developer Handbook](docs/UP-SELF-Developer-Handbook.md) for architecture, roles, workflows, setup, migration, deployment costs and the complete 61-FR comparison. A [paginated Word edition](docs/UP-SELF-Developer-Handbook.docx) is available. The [documentation index](docs/README.md) links to individual chapters.

## Windows startup

Configure the fixed tool paths with scripts/configure-local.ps1, install the locked frontend dependencies and build. Open open-vscode.cmd, which selects the generated .local/up-self.code-workspace. Wait for Java import, then use the existing Backend + Frontend configuration and F5. Do not open another backend or Vite instance on the same ports.

```powershell
.\frontend.cmd install
.\build.cmd
.\frontend.cmd build
.\open-vscode.cmd
```

Open **http://127.0.0.1:5173/**. The backend on 8080 is not the Vue homepage. MySQL and Redis must be available; the backend prelaunch task prepares their existing service workflow and Maven XML/SQL resources.

## Mac preparation

Follow the handbook's [Mac prerequisites](docs/UP-SELF-Developer-Handbook.md#58-m1-mac-prerequisites). Mac uses separate scripts/macos/up-self.sh helpers and a generated .local/macos/up-self.code-workspace. The intended M1 Mac requires a macOS release supported by Node 24. Mac runtime testing remains pending; Windows operation is unchanged.

## Configuration and data

Current local service settings and original bootstrap credentials are in config/application-local.yml. Current demo logins are listed in [config/demo-accounts.yml](config/demo-accounts.yml): student01–student06, mentor01–mentor02 and admin01 all use `1234`. The internal maintenance account is unchanged. The demo inventory is for reference; editing it does not change the hashed passwords stored in MySQL. Bootstrap values apply only when initializing an empty database.

Public roles are Student, Mentor and Administrator; maintenance uses only the internal login entry. Password login is enabled and TOTP is disabled. The six demo students have course progress, submitted and reviewed work, goals, mood and self-belief history, and course discussion messages. These are synthetic demonstration records.

Current schema is **V12**. Java startup applies missing Flyway migrations. Git transfers code, not live database writes. The local migration snapshot has been refreshed with the current demo accounts, data and referenced learning media. Export it again after making further database or upload changes. See [setup and data migration](docs/UP-SELF-Developer-Handbook.md#5-local-setup-and-data-migration).

## Versions and deployment

Retain Java 8u231, Maven 3.10.0, MySQL 5.7.44, the Windows Redis 5.0.14.1 port, Node 24.22.1 and npm 11.19.1. Backend dependencies follow the existing POMs and frontend dependencies follow package-lock.json. Full versions and the Mac Redis platform mapping are in the handbook.

Linux production templates are prepared in deploy/linux; actual Linux runtime validation and public deployment are pending. No cloud purchase is required for local development or a local graduation demonstration.
