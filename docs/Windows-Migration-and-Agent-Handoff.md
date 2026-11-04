# Windows Migration and Development Handoff

Updated: 4 November 2026 · schema V19 · inspected committed baseline `96181b1` plus local questionnaire changes

This guide moves UP-SELF and its data to another Windows computer and gives the next developer a starting point. It covers local development and demonstration, not public deployment. Writing this guide has not exported data, installed software or tested the destination.

**UP-SELF is now the engineering baseline. The original reference project is not required.** Do not copy it to the new computer or make development depend on it. Follow this repository's code, locked dependencies, business rules and Developer Handbook. This supersedes older handoff instructions requiring the reference project. Keep the current architecture and versions; this is not authorization for an unrelated redesign or upgrade.

## 1. What must move

1. **Project and configuration:** the current repository, actual application settings and existing migration credentials.
2. **Database:** a fresh logical export of `up_self_dev`, taken after stopping application writes.
3. **Private files:** the complete active upload tree from the same stopped application, including course media, assignments, chat attachments and recordings.

Git does not transfer live database writes or `.local/uploads`. Redis sessions need not move; sign in again after restoration. Keep the working source intact until the destination passes section 7. Two independent restored databases do not synchronize. Ongoing shared live data eventually requires one shared deployed backend.

### Current snapshot-tool limitation

`scripts/database.ps1 -Action Export` dumps every database table, but its attachment collector queries only `learning_resource` and `assignment_file`. It omits `chat_file` and `mood_entry.recording_key`. It also assumes assignment files are under `uploads/assignments`, while assignment audio is stored under `uploads/recordings` through `RecordingStorage`.

**Do not rely on that export alone after using chat or recordings.** It may omit files or fail when looking for assignment audio in the wrong directory. The complete workflow below uses a separate logical dump and the entire upload tree. It does not change the script or edit a snapshot manifest.

The archived snapshot inspected for this guide is dated **30 October 2026**, records migrations **1–17**, and lists **12 collected uploads**. This does not prove all current chat/recording files are included. It is a dated archive, not a live mirror.

## 2. Destination environment

Use Windows x64 and the established versions:

| Component | Baseline | Purpose |
|---|---|---|
| Oracle JDK | 1.8.0_231; JDK, not only JRE | Backend compilation and runtime |
| Maven | 3.10.0 | Backend build |
| MySQL server and tools | 5.7.44 | Database, dump and restore |
| Redis Windows port | tporadowski 5.0.14.1 | Sessions and temporary state |
| Node.js / npm | 24.22.1 / 11.19.1 | Frontend dependencies and development |
| VS Code | Working installation with `code` command | Workspace and debugging |
| Java extensions | Extension Pack for Java, including Debugger for Java | Java import and F5 |
| Git | Windows installation | History and future updates |
| Chrome | Working installation | Acceptance and recording tests |
| Python | Optional; existing tools prepared with 3.12 | Specialist regression checks |

Use the sources in [Handbook 5.3](UP-SELF-Developer-Handbook.md#53-download-the-fixed-tools), or transfer existing Windows x64 installer archives privately. Installers are not in Git. If an exact version is unavailable, record the problem instead of installing a default latest version. A dependency cache does not replace a runtime installation.

Example directories; replace with real paths on an existing drive:

```text
D:\projects\UP-SELF
D:\dev\jdk1.8.0_231
D:\dev\apache-maven-3.10.0
D:\dev\node-v24.22.1-win-x64
D:\dev\mysql-5.7.44-winx64
D:\dev\Redis-x64-5.0.14.1
```

Do not reproduce the old Windows username. Do not copy a running MySQL physical data directory, Windows service registration or the source `.local` folder as the destination setup. VS Code's language server can use the newer runtime supplied with its extension; the application remains on Java 8u231.

## 3. Source computer: make a complete transfer package

Run this when ready to move the latest records. Use a private folder or drive. This workflow neither pushes to GitHub nor purchases a server.

### 3.1 Stop writes and choose paths

Save edits. Stop VS Code Backend + Frontend debugging and any terminal-launched copies. Keep MySQL running. Do not change application records or run migrations until the package is finished.

Open PowerShell. These source paths match the inspected computer. Change them if necessary. The transfer drive must exist and have enough space.

```powershell
$ErrorActionPreference = 'Stop'
$sourceProject = 'C:\Users\eggma\Desktop\codex\up-self'
$sourceMySql = 'C:\Users\eggma\Documents\Codex\local-env\up-self\mysql'
$packet = Join-Path 'D:\UPSELF-transfer' (Get-Date -Format 'yyyyMMdd-HHmmss')
if (Test-Path -LiteralPath $packet) { throw 'Choose a new transfer folder.' }
New-Item -ItemType Directory -Path $packet | Out-Null
Set-Location -LiteralPath $sourceProject
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Check -MySqlHome $sourceMySql
if ($LASTEXITCODE -ne 0) { throw 'Source database check failed.' }
$rootClient = Join-Path $sourceProject '.local\migration-root-client.cnf'
& "$sourceMySql\bin\mysql.exe" "--defaults-extra-file=$rootClient" -N -B --database=up_self_dev -e 'SELECT @@version, DATABASE();'
if ($LASTEXITCODE -ne 0) { throw 'Source server verification failed.' }
```

Confirm actual server 5.7.44 and database `up_self_dev`. The check writes a local root client option file but does not modify database records. The owner's root password remains `root`; do not reset the source account to resolve a connection error.

### 3.2 Dump all tables

```powershell
$dumpFile = Join-Path $packet 'up_self_dev.sql'
& "$sourceMySql\bin\mysqldump.exe" "--defaults-extra-file=$rootClient" --single-transaction --quick --routines --triggers --events --hex-blob --set-gtid-purged=OFF --no-tablespaces --default-character-set=utf8mb4 "--result-file=$dumpFile" up_self_dev
if ($LASTEXITCODE -ne 0) { throw 'Export failed; do not use this package.' }
if ((Get-Item -LiteralPath $dumpFile).Length -eq 0) { throw 'Empty database export.' }
$sourceCommit = & git -c "safe.directory=$sourceProject" -C $sourceProject rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Could not record source commit.' }
$sourceHistory = & "$sourceMySql\bin\mysql.exe" "--defaults-extra-file=$rootClient" -N -B --database=up_self_dev -e 'SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;'
if ($LASTEXITCODE -ne 0) { throw 'Could not record migration history.' }
(@("Exported: $(Get-Date -Format o)", "Commit: $sourceCommit", 'Database: up_self_dev', 'Flyway history:') + @($sourceHistory)) | Set-Content -LiteralPath (Join-Path $packet 'source-state.txt') -Encoding UTF8
```

`--result-file` preserves the dump bytes. Do not replace it with a PowerShell text pipeline. All tables are included, including account hashes, journals, messages and Flyway history; file bytes remain separate.

### 3.3 Copy code and the entire upload tree

Confirm the active `learning.storage-path`, `assignment.storage-path`, `chat.storage-path` and `recording.storage-path`. The inspected setup uses `.local/uploads`, with `assignments`, `chat` and `recordings` below it. **For any overridden external path, back up that real directory separately and restore it to the corresponding destination setting.**

This copy includes `.git`, real configuration and local edits, excluding rebuildable output and machine-specific `.local` state. Review uncommitted edits first; a commit ID does not describe them.

```powershell
& robocopy $sourceProject (Join-Path $packet 'project') /E /R:1 /W:1 /XD .local node_modules target dist logs test-results playwright-report /XF *.log /NFL /NDL /NJH /NJS
if ($LASTEXITCODE -ge 8) { throw 'Project copy failed.' }
$sourceUploads = Join-Path $sourceProject '.local\uploads'
if (-not (Test-Path -LiteralPath $sourceUploads -PathType Container)) { throw 'Find the active uploads before continuing.' }
& robocopy $sourceUploads (Join-Path $packet 'private-uploads') /E /R:1 /W:1 /NFL /NDL /NJH /NJS
if ($LASTEXITCODE -ge 8) { throw 'Upload copy failed.' }
```

Robocopy codes 0–7 are not failures; 8 or higher is a failure. `/E` copies without deleting to force a match. Do not substitute `/MIR`.

Expected package:

```text
<package>/
  project/                         repository, history and current configuration
    config/application-local.yml
    config/demo-accounts.yml
    migration/local-snapshot/local-credentials.json
    docs/
  up_self_dev.sql                  NEW live database export
  private-uploads/                 ALL active media, including chat and recordings
  source-state.txt
  transfer-manifest.json           created below
```

The older dump inside `project/migration/local-snapshot` is not the new top-level dump. Do not mix them.

### 3.4 Hash and transfer

Keep writes stopped while hashing. This covers code, configuration, dump and copied uploads. Hashes detect transfer changes; they cannot recover something already missing at source.

```powershell
$packetPrefix = [IO.Path]::GetFullPath($packet).TrimEnd('\') + '\'
$manifestRows = @(Get-ChildItem -LiteralPath $packet -File -Recurse -Force | Where-Object { $_.FullName -ne (Join-Path $packet 'transfer-manifest.json') } | ForEach-Object {
    [pscustomobject]@{
        path = $_.FullName.Substring($packetPrefix.Length).Replace('\', '/')
        bytes = $_.Length
        sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
})
$manifest = [ordered]@{ createdAt = (Get-Date -Format o); database = 'up_self_dev'; sourceCommit = [string]$sourceCommit; files = $manifestRows }
$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $packet 'transfer-manifest.json') -Encoding UTF8
Write-Host "Transfer package ready: $packet"
```

Copy the entire package privately to the new computer. After hashing, the source may resume work, but later writes are outside this package. Keep the source and package until acceptance succeeds.

For offline preparation, separately transfer fixed tool installers and optionally `.local/maven-repository`. Do not transfer generated workspaces, raw MySQL data or Redis persistence as destination state. Missing uncached dependencies still require a network connection.

## 4. Destination: validate and configure tools

### 4.1 Verify before opening Git or VS Code

Opening tools can change `.git` metadata. First validate the untouched received package:

```powershell
$ErrorActionPreference = 'Stop'
$packet = 'D:\UPSELF-transfer\REPLACE_WITH_PACKAGE_FOLDER'
$manifest = Get-Content -LiteralPath (Join-Path $packet 'transfer-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$packetPrefix = [IO.Path]::GetFullPath($packet).TrimEnd('\') + '\'
foreach ($entry in $manifest.files) {
    $filePath = [IO.Path]::GetFullPath((Join-Path $packet $entry.path))
    if (-not $filePath.StartsWith($packetPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid transfer path.' }
    if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) { throw "Missing: $($entry.path)" }
    if ((Get-Item -LiteralPath $filePath).Length -ne $entry.bytes) { throw "Size mismatch: $($entry.path)" }
    if ((Get-FileHash -LiteralPath $filePath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Hash mismatch: $($entry.path)" }
}
Write-Host 'Transfer integrity passed.'
```

### 4.2 Copy into a fresh project directory

```powershell
$project = 'D:\projects\UP-SELF'
if (Test-Path -LiteralPath $project) { throw 'Preserve the existing directory; choose a new one.' }
& robocopy (Join-Path $packet 'project') $project /E /R:1 /W:1 /NFL /NDL /NJH /NJS
if ($LASTEXITCODE -ge 8) { throw 'Destination project copy failed.' }
Set-Location -LiteralPath $project
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure-local.ps1 -JavaHome 'D:\dev\jdk1.8.0_231' -MavenHome 'D:\dev\apache-maven-3.10.0' -NodeHome 'D:\dev\node-v24.22.1-win-x64'
if ($LASTEXITCODE -ne 0) { throw 'Tool configuration failed.' }
```

Use real installation paths. JavaHome/MavenHome contain `bin`; Windows NodeHome contains `node.exe`. The script validates versions and generates `.local/development-tools.json` and `.local/up-self.code-workspace` without changing system PATH or passwords.

A fresh clone is an alternative, but must match `source-state.txt` and include the source's local edits and configuration. A clone alone is not a complete migration. This guide requires no push.

## 5. Destination: create services and restore data

### 5.1 Start the intended MySQL and Redis instances

The straightforward route is a new project-local data directory using fixed unpacked binaries. Check ports 3306 and 6379 first. If occupied, identify and preserve the existing service; do not reinitialize it or stop another project's service blindly.

```powershell
$mysqlHome = 'D:\dev\mysql-5.7.44-winx64'
$redisHome = 'D:\dev\Redis-x64-5.0.14.1'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure-services.ps1 -MySqlHome $mysqlHome -RedisHome $redisHome
if ($LASTEXITCODE -ne 0) { throw 'Service configuration failed.' }
```

This reads the transferred migration credentials and generates `.local/my.ini`, `.local/redis.conf` and `.local/service-tools.json`. It refuses to overwrite an existing local service setup or MySQL data directory. It does not initialize MySQL or reset root.

Only for this **new, empty data directory**:

```powershell
& "$mysqlHome\bin\mysqld.exe" "--defaults-file=$project\.local\my.ini" --initialize --console
if ($LASTEXITCODE -ne 0) { throw 'New MySQL initialization failed.' }
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\services.ps1
if ($LASTEXITCODE -ne 0) { throw 'Local service startup failed.' }
& "$mysqlHome\bin\mysql.exe" -h 127.0.0.1 -u root -p --connect-expired-password
```

Use the displayed temporary password. In the MySQL console, set the intended password for this newly created local root account, then exit:

```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
EXIT;
```

This retains the owner's local convention. Do not run initialization or this password change against an existing instance. If reusing a correct existing instance, use its actual root credential through a private `-RootClientFile` and verify the app/migration accounts. Do not reset it just to follow an example.

Redis takes its password from the transferred settings and uses DB 2. No Redis restoration is needed for application records; sign in again.

### 5.2 Prepare an empty database and scoped users

**Do not start Java yet:** it would create tables before the full import.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Init -MySqlHome $mysqlHome
if ($LASTEXITCODE -ne 0) { throw 'Database preparation failed.' }
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Check -MySqlHome $mysqlHome
if ($LASTEXITCODE -ne 0) { throw 'Destination database check failed.' }
```

Expect `up_self_dev` to have **0 tables**. `Init` creates the database and scoped app/migration users, without executing Flyway or resetting existing passwords. This guide uses **Init followed by the new full dump**, not the archived snapshot Restore workflow. Do not run both alternatives.

### 5.3 Import SQL and restore all uploads

MySQL reads the file through its own `source` command. For the command below, put the transfer package in a path without whitespace or semicolons.

```powershell
$rootClient = Join-Path $project '.local\migration-root-client.cnf'
$dumpPath = (Join-Path $packet 'up_self_dev.sql').Replace('\', '/')
if ($dumpPath -match '[\s;]') { throw 'Choose a transfer path without whitespace or semicolons.' }
& "$mysqlHome\bin\mysql.exe" "--defaults-extra-file=$rootClient" --default-character-set=utf8mb4 --database=up_self_dev "--execute=source $dumpPath"
if ($LASTEXITCODE -ne 0) { throw 'SQL import failed; preserve the error and partial target.' }

$uploadTarget = Join-Path $project '.local\uploads'
if ((Test-Path -LiteralPath $uploadTarget) -and @(Get-ChildItem -LiteralPath $uploadTarget -File -Recurse -Force).Count -ne 0) { throw 'Uploads already exist; do not merge over them.' }
& robocopy (Join-Path $packet 'private-uploads') $uploadTarget /E /R:1 /W:1 /NFL /NDL /NJH /NJS
if ($LASTEXITCODE -ge 8) { throw 'Upload restoration failed.' }
foreach ($entry in @($manifest.files | Where-Object { $_.path -like 'private-uploads/*' })) {
    $relativeFile = $entry.path.Substring('private-uploads/'.Length)
    $restoredFile = Join-Path $uploadTarget $relativeFile
    if (-not (Test-Path -LiteralPath $restoredFile -PathType Leaf)) { throw "Missing restored upload: $relativeFile" }
    if ((Get-FileHash -LiteralPath $restoredFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Upload differs: $relativeFile" }
}
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Check -MySqlHome $mysqlHome
if ($LASTEXITCODE -ne 0) { throw 'Post-restore check failed.' }
```

Successful history should match `source-state.txt`; the current code baseline is V1–V19; match the actual package history. Keep SQL and files from the same package. If import fails, preserve the error and partial target, determine the cause, then prepare a fresh independent target as appropriate. Do not repeatedly import into partial tables or delete Flyway history.

Actual app/migration credentials stay in their established files. Investigate mismatches instead of resetting every password. Editing `config/demo-accounts.yml` does not change password hashes in MySQL.

## 6. Build and start in VS Code

From the destination project root:

```powershell
.\frontend.cmd install
if ($LASTEXITCODE -ne 0) { throw 'Frontend installation failed.' }
.\build.cmd
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
.\frontend.cmd build
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
.\open-vscode.cmd
```

Install uses `npm ci` and the lockfile. Maven caches dependencies in `.local/maven-repository`. The first build may download missing dependencies. Do not run npm update or replace the lockfile to fix a download failure.

In VS Code:

1. Open generated **`.local/up-self.code-workspace`** using `open-vscode.cmd`.
2. Handle its workspace trust prompt yourself and wait for Java import.
3. Choose **UP-SELF · Backend + Frontend**. VS Code may append `(workspace)` to the label.
4. Press F5. **UP-SELF prepare backend** checks/starts the configured services and copies Mapper XML/Flyway SQL through Maven `process-resources`.
5. Open **<http://127.0.0.1:5173/>**. Port 8080 is the API, not the Vue home page.

The root Windows workspace is a template; `.local/up-self.code-workspace` holds new-machine paths. The macOS workspace is not used here. Do not copy the old generated workspace or choose Debug Anyway to bypass a failed preparation task.

Flyway checks history during backend startup and applies missing migrations once. A current full dump includes history, so already recorded versions should not run again. Vue startup does not execute Flyway.

Alternative terminal workflow: run `start.cmd` for the built backend and `frontend.cmd dev` in a second terminal. Rebuild after Java changes. Do not run these alongside F5 on the same ports.

## 7. Destination acceptance

Before retiring the source, check:

- [ ] Exact tool versions passed configure-local; the actual MySQL server and Redis are the intended versions and authenticate correctly.
- [ ] Imported Flyway history matches the source and startup reports no checksum failure.
- [ ] Transfer integrity and restored upload hashes pass.
- [ ] VS Code starts both services without bypassing its task.
- [ ] `student01`, `mentor01` and `admin01` can sign in; their current demo password is `1234` unless changed in the live database.
- [ ] Students see their joined published courses, assignments, goals, bookmarks and check-in history.
- [ ] Play an existing video, view an image/caption, open a past assignment attachment, play a diary recording and a group voice message wherever those records exist at source.
- [ ] Record, preview and upload a new short recording in Chrome; configure microphone permission if required. No transcription service is needed.
- [ ] Mentors can access their current students and teaching workflows, and cannot access unrelated students.
- [ ] Administrator and mentor homes/menus differ; maintenance is internal only.
- [ ] Encouragement inbox and sender history survive refresh; another student's inbox is denied.
- [ ] Aa font/contrast/motion options, keyboard focus and small-screen layout remain usable.
- [ ] New data survives backend/frontend restart.

Acceptance must be performed on the destination. The previous Windows delivery recorded **187 Java tests, 118 frontend tests and 159 FR-59 specialist checks** on 30 October; these are historical results, not new-machine test evidence.

After setup, useful checks include:

```powershell
.\frontend.cmd test
.\frontend.cmd typecheck
.\frontend.cmd lint
```

`build.cmd` runs Maven package including backend tests. Previous full lint had zero errors and 48 existing warnings; do not disable rules to conceal new problems.

**Existing browser/integration scripts need a portability check:** `scripts/verify-local.py` embeds `C:\Users\eggma\Documents\Codex\local-env\up-self` and reads helper credentials there. Several feature verifiers import it. The next agent must inspect and narrowly adapt this lookup before using these verifiers under a different user/path, reusing current local configuration and preserving disposable-fixture cleanup. Normal application startup does not require recreating the old user's directory. Some Playwright tests require a fixture from their Python verifier and otherwise skip; a skip is not a verified business workflow.

## 8. Development handoff for the next agent

Read this guide, then [Developer Handbook](UP-SELF-Developer-Handbook.md), especially architecture, roles, feature workflows and Appendix A/B. Inspect current Git status, actual configuration, migration files and the implementation closest to the requested feature. Preserve local edits; do not assume this guide's commit is still HEAD.

### Code map

| Area | Location |
|---|---|
| Application and controllers | `up-self-admin/src/main/java/com/upself/web/` |
| Flyway migrations | `up-self-admin/src/main/resources/db/draft/` |
| Shared API contracts | `up-self-common/src/main/java/com/upself/common/core/` |
| Security and infrastructure | `up-self-framework/` |
| User administration | `up-self-system/` |
| Business code | `up-self-biz/src/main/java/com/upself/biz/` and its resource Mapper XML |
| Frontend routing | `frontend/src/app/router.ts` |
| Main role layout | `frontend/src/app/layouts/AppLayout.vue` |
| Feature pages/components/API/types | `frontend/src/features/` |
| Shared English messages | `frontend/src/shared/i18n/en.ts` |
| Setup and verification | `scripts/` |
| Maintained documentation | `docs/UP-SELF-Developer-Handbook.md` |

The backend is one Spring Boot application assembled from six Maven modules: admin, framework, system, common, biz and the retained aliyun-oss boundary. Object storage is not enabled. MySQL stores records, Redis holds session/protection state, and authorized backend services serve private files. The frontend is one Vue application with dedicated student and simple management experiences.

Calls follow Controller → Service → Mapper/XML. Reuse BaseController, AjaxResult, TableDataInfo, PageHelper, exception handling, service authorization and transaction boundaries. Run identity checks **before** startPage so they do not consume list pagination. Follow the existing feature-oriented Vue organization; avoid a second response format, pagination library or generic rule engine.

### Preserve these decisions

- Public Student, Mentor and Administrator roles have distinct homes/menus; maintenance is internal. Password login is enabled, TOTP disabled, other security retained.
- `/client/login` and `/empLogin` use separate channels. Enforce role, channel, ownership, enrollment, publication and current mentor relationships on the backend, not just hidden buttons.
- User-visible text is English with existing i18n boundaries. Short labels, icon-plus-text, keyboard access, Aa and clear hierarchy remain essential. Reuse existing menus.
- Chapter completion is self-reported, not watched-minute or mastery proof. Assignment resubmission requires mentor return.
- Mood is weekly. Journals and recordings, including earlier test journals, are shared read-only with eligible current mentors. Author-private mentor observations are separate and do not transfer to a new mentor.
- GSE-3 retains its original wording, attribution, license and 28-day application interval; mentors see scores, not individual answers.
- Per-group peer hiding is unilateral and must preserve mentor teaching messages. Filtering is literal text/name checking, not AI moderation or audio analysis.
- Encouragement is one-way text, recipient-private, all-or-nothing for batches and idempotent. Delivered history remains with the student and original sender after reassignment.
- Preserve encouragement Mapper `byKey` cache-bypass settings: they fix a stale MyBatis session-cache result after lock waits. Do not casually remove them or alter global caching.
- Keep private files behind authorization. Do not turn storage keys into public download capabilities.
- Do not reset root, demonstration passwords or seed data as a routine startup fix.

### Schema and remaining scope

The inspected code has V1–V19. **V20 is next only if the current tree still ends at V19.** Never edit executed migrations, delete history or run clean/blind repair to make startup pass. Keep schema changes with their Java/Mapper implementation.

`environment/versions.json` contains tool baselines but still has historical `flywaySchemaVersion: 9` and an incomplete feature list. Do not use it as schema/feature-status authority. Use actual migration files, successful database history and the handbook matrix. The published Word edition is dated 25 October; Markdown contains later updates.

Current FR status is **41 complete, 6 partial, 14 unimplemented**, after the badge Chrome acceptance passed. Use all 61 rows in Appendix A before planning work. Accepted weekly mood, manual progress and controlled resubmission differences are not automatic requests for extra features. Formal teaching content and entrepreneur stories will be supplied by users; do not fabricate them to mark requirements complete.

**FR-26 was authorized on 3 November and its code is implemented.** Three one-time milestones, V18 evidence-based backfill, private student API, the Learning stats panel and inline action messages are present. Backend/API/rollback checks passed. The optional local WASM mode now also passes 129 frontend tests, production build and three-role Chrome acceptance on this cloud Windows. Follow handbook 4.24 and run `scripts/verify-badges.py --browser`; the local source frontend is verified; the destination computer still needs its own startup checks. The 2 November transfer ZIP is an immutable older V17 backup. Create a new package for V18 rather than editing its old manifest.

After an authorized feature change, run appropriate builds/tests, real browser workflows and direct unauthorized requests, verify VS Code startup, and update the maintained handbook/FR row. Report fresh evidence and limitations. Do not push, buy services, upgrade dependencies or deploy unless asked.

## 9. Troubleshooting

| Symptom | Check |
|---|---|
| Fixed tool version rejected | Select the real matching tool directory; do not weaken validation to hide the mismatch. |
| `code` not found | Install VS Code's command entry and reopen PowerShell. |
| Missing prelaunch task | Open the generated Windows workspace and reload; do not bypass the task. |
| Occupied ports | Identify the owning instance before stopping it; do not kill all Java/Node processes. |
| MySQL access denied | Check server identity, client option file, app/migration accounts and settings. Existing account passwords are not reset by CREATE USER IF NOT EXISTS. |
| Redis NOAUTH/WRONGPASS | Match active settings and DB 2; do not flush Redis. |
| New API/XML/SQL unavailable | Check the current Java build and resource-preparation task, then restart the backend. |
| Media or voice unavailable | Check complete uploads, especially chat/recordings, and current authorization. SQL alone cannot restore file bytes. |
| Interrupted SQL import | Preserve the partial target and error; investigate before a fresh controlled retry. |
| New permission missing | Confirm migration success, restart the latest backend and sign out/in. |
| Verifier reads old username paths | Adapt its local configuration lookup; application startup does not need those paths. |
| Git dubious ownership | Verify the exact project; a scoped `git -c safe.directory=D:/projects/UP-SELF ...` is available. Do not trust every directory globally. |
| Browser settings differ | Aa personal choices, voices and microphone permissions are local to that browser/device, separate from MySQL data. |

## 10. Prompt for the next agent

> Read `docs/Windows-Migration-and-Agent-Handoff.md` and the maintained Developer Handbook. UP-SELF itself is the engineering baseline; the old reference project is not needed. Inspect current Git status, real tool versions, configuration, Flyway history and private upload paths. Complete or verify migration without overwriting existing data. Keep current architecture, versions, backend authorization and English inclusive student UI. Do not claim success until this Windows computer actually builds, starts in VS Code and passes browser checks. Some Python verifiers still need a narrow old-path portability fix. FR-26 code is implemented in V18, but its frontend build/browser acceptance has passed through the local WASM mode; verify the destination environment after migration. Do not add other features, push, upgrade or deploy unless asked.

## 11. Platform questionnaires added on 4 November

V19 adds global questionnaire links, without course/enrollment scope. All students can read visible links under My check-in > Questionnaires. Mentors manage only their own links from Home or Students, with default-hidden creation, edit, visibility and delete/version controls. No external responses are stored. This is a teacher-requested addition outside the original numbered FR matrix. Read handbook 4.25/B16 and run scripts/verify-questionnaires.py --browser after frontend tools work on the destination. Backend/API verification passed on the source; frontend acceptance subsequently passed through the local WASM compatibility mode. Sign out/in after applying the new permissions. The dated 2 November transfer ZIP is unchanged V17 and does not contain badge/questionnaire code; export a fresh complete package to move the current V19 state.

WASM compatibility update, 4 November: this cloud Windows now uses the opt-in `.local/esbuild-wasm.enabled` mode with official esbuild-wasm 0.28.2. Other machines retain native startup when the marker is absent. See Developer Handbook 5.16. All 129 frontend tests, production build, actual F5 launch and Chrome three-role badge/questionnaire workflows passed. Earlier pending-acceptance statements describe the initial native compiler restriction and are superseded by this verification. Refresh a migration package to include the current code; existing dated ZIPs are unchanged.
