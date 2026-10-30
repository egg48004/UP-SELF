# UP SELF Developer Handbook

Development standards, feature workflows, local setup and deployment operations

Version 1.4 | 30 October 2026 | Database schema V16

UP-SELF is a learning platform for a small group of students in Malaysia. This handbook explains how the current application works, how to maintain it, and how to run it on another computer. It covers the Java backend, Vue frontend, permissions, private files, database migrations and release preparation.

The application has working course, assignment, weekly mood, personal goal, bookmark and self-efficacy workflows. Windows development has been tested. Mac startup support is prepared and needs testing on the target M1 Mac. Linux deployment templates are prepared; Linux runtime testing and public deployment remain pending.

Maintain this Markdown file as the source. Regenerate the Word edition after substantive changes. Dependency manifests and application configuration remain in their existing project locations. Examples describe the current interfaces; sample IDs and dates are illustrative.

## 1 Project overview

### 1.1 Scope and current status

The initial audience is 10 to 30 students, primarily in Malaysia. Students use a dedicated learning layout. Mentors and administrators use simpler management screens, with different home pages, menus and permissions. An internal maintenance account supports administration tasks and is not a fourth public role.

| Area | Current behavior |
|---|---|
| Accounts and login | Password login, separate student and management channels, Redis sessions, role checks and account revocation |
| Courses | Administrator creates courses, assigns one active mentor, enrolls students and controls publication |
| Learning content | Assigned mentors maintain chapters, images, videos, captions, transcripts and course posts |
| Assignments | Mentor publishes work; student saves a draft and submits; mentor grades or returns it; student reads feedback |
| Weekly mood | Student records once per Malaysia calendar week and views history; eligible current mentors have read-only access |
| Learning progress | Last chapter, explicit completion, resume, completion history and a mentor student overview |
| Personal goals | Student creates, edits, completes, reopens and deletes private goals |
| Course bookmarks | Student saves accessible courses, removes bookmarks and views saved courses |
| Self-efficacy | GSE-3 assessment, first-use disclosure, 28-day interval, history and trends; eligible mentors see scores only |
| Course discussions | Mentor-managed groups; scoped text, images, documents and voice messages |
| Mentor observations | Author-private follow-up notes for currently assigned students |
| Business practice | Private guided ideas and one saved MYR batch-cost plan per student |
| Reports | Student and mentor report another chat sender; administrator reviews private message evidence |
| Follow-up reminders | Current mentors review three fixed learning and wellbeing signals |
| CSV exports | Administrator downloads users, courses and learning totals with filters |
| Platform reading defaults | Administrator sets starting size, contrast and reduced motion; personal browser choices take priority |
| Learning statistics | Student sees current-course chapter completion and assignment totals within My learning |

The original graduation report contains 61 functional requirements. The current comparison records 39 completed, 6 partially completed and 16 not implemented. Appendix A retains all 61 identifiers and explains the differences. These counts measure coverage of the report, not the usability of the MVP or an academic grade.

Course discussions, private mentor notes, guided business ideas and a saved cost calculator are implemented. AI assistance, badges, daily attendance and full one-to-one appointment scheduling remain outside the implemented scope. A course meeting post is a link announcement, not a booking or conferencing service.

### 1.2 Runtime model

The frontend is one Vue application. The backend is one Spring Boot application assembled from six Maven modules. MySQL stores application records. Redis supports login sessions, login protection and temporary media tickets. Uploaded files remain on a private filesystem controlled by backend authorization.

During local development, the browser opens Vite on port 5173. Requests under `/api` are forwarded to the backend on port 8080, with `/api` removed. The backend connects to MySQL on 3306 and Redis on 6379, using Redis database 2. The backend root page is a legacy account verification page, not the Vue learning interface.

For a future public release, Nginx serves the built Vue files and forwards API requests to Java. MySQL, Redis and private uploads can remain on the same small Linux server. This is a proposed deployment of the existing monolith; it has not been purchased or deployed.

### 1.3 Repository layout

| Path from repository root | Responsibility |
|---|---|
| `up-self-admin/` | Application entry point, controllers, application resources, Flyway scripts and backend tests |
| `up-self-framework/` | Security, token services, datasource setup, MyBatis integration and exception handling |
| `up-self-system/` | User administration services and MyBatis mappings |
| `up-self-common/` | Shared responses, base controller, pagination, identity objects and utilities |
| `up-self-biz/` | Learning, assignments, mood, goals, bookmarks, progress and self-efficacy business code |
| `up-self-aliyun-oss/` | Retained module boundary; application object storage is not enabled |
| `frontend/` | Vue application, locked npm dependencies and frontend tests |
| `config/` | Local application configuration and example configuration |
| `scripts/` | Existing Windows tools, verification scripts and separate Mac helpers |
| `deploy/linux/` | Linux launch, Nginx, service, environment and backup templates |
| `migration/local-snapshot/` | A dated logical database snapshot, matching uploads and integrity manifest |
| `.local/` | Ignored machine paths, generated workspaces, uploads, caches and temporary verification output |
| `docs/` | This handbook, its Word edition and retained dependency evidence |

Paths in this handbook are relative to the repository unless stated otherwise. A path in a fenced command uses that command's operating system syntax.

### 1.4 Fixed versions

The backend baseline comes from the reference project at `C:\Users\eggma\Desktop\project\fts_admin_ac`. Its source is a read-only architecture reference. The independent UP-SELF code and its resolved dependencies define the current implementation. The reference had no Vue frontend; frontend versions were selected and locked separately during UP-SELF development.

| Runtime or tool | Current baseline |
|---|---|
| Application JDK | Oracle JDK 1.8.0_231 |
| Maven | 3.10.0 |
| MySQL server | 5.7.44 |
| Redis on Windows | tporadowski Windows port 5.0.14.1 |
| Redis for Mac preparation | Upstream 5.0.14 corresponding to the Windows port; target runtime testing pending |
| Node and npm | 24.22.1 and 11.19.1 |
| Current Flyway schema | V1 through V16 |

The reference Docker image names Amazon Corretto 8u362. That differs from the independent project's installed Oracle 8u231. Do not describe them as the same JDK build or silently switch vendor or update level. The current Linux runtime checks retain the independent project's 8u231 requirement.

| Backend component | Resolved version |
|---|---|
| Spring Boot | 2.5.14 |
| Spring Framework | 5.3.20 |
| Spring Security | 5.5.8 |
| MyBatis and MyBatis Spring | 3.5.9 and 2.0.7 |
| MyBatis Spring Boot starter | 2.2.2 |
| MyBatis Plus | 3.3.1 |
| MyBatis Plus Spring Boot starter | 1.0.5 |
| PageHelper and starter | 5.3.0 and 1.4.1 |
| Druid | 1.2.11 |
| MySQL Connector J | 8.0.29 |
| Flyway | 7.7.3 |
| Lettuce | 6.1.8.RELEASE |
| JJWT | 0.9.1 |
| Fastjson2 | 2.0.57 |
| Jackson databind | 2.12.6.1 |
| Hibernate Validator | 6.2.3.Final |
| SLF4J and Logback | 1.7.36 and 1.2.11 |
| Embedded Tomcat | 9.0.63 |
| Lombok | 1.18.24 |

The packaged runtime also contains transitive libraries. Presence in a JAR does not mean the application actively uses every component: for example, HikariCP is present while configured connections use Druid. The full recorded JAR list is [runtime-dependencies.json](runtime-dependencies.json). Consult the POMs, resolved dependency tree and packaged JAR when reconciling a version.

| Frontend component | Locked version |
|---|---|
| Vue | 3.5.44 |
| TypeScript | 5.9.3 |
| Vite and Vue plugin | 7.3.7 and 6.0.10 |
| Vue Router and Pinia | 4.6.4 and 3.0.4 |
| Axios | 1.20.0 |
| Element Plus | 2.14.7 |
| Vue I18n | 11.4.16 |
| vue-tsc | 3.3.13 |
| Vitest and Vue Test Utils | 4.1.12 and 2.5.1 |
| jsdom and Playwright | 27.4.0 and 1.64.1 |
| ESLint | 9.39.5 |
| eslint-plugin-vue and typescript-eslint | 10.11.1 and 8.72.0 |
| Prettier | 3.9.10 |

`frontend/package-lock.json` is the installation authority. [frontend-dependency-versions.json](frontend-dependency-versions.json) retains the installation-time metadata, including engine and peer requirements. Optional peers listed there are not instructions to install more packages.

## 2 Architecture and development standards

### 2.1 Maven organization

Keep the existing six-module reactor. The root POM imports Spring Boot dependency management and defines shared version properties and internal module versions. Child POMs declare their own required dependencies. A version already managed by the parent or BOM should not be repeated in an individual business module.

The compiler plugin is `maven-compiler-plugin` 3.1, with source and target 1.8 and UTF-8 encoding. The admin module packages the runnable Spring Boot JAR using `spring-boot-maven-plugin` **2.1.1.RELEASE**, as declared in its POM; this differs from the Spring Boot runtime version 2.5.14. It also declares `maven-war-plugin` 3.1.0, although the normal application output is a JAR. The root property for `maven-jar-plugin` is 3.1.1; a property alone is not evidence that a plugin runs. Keep these actual declarations rather than normalizing their versions during documentation work.

Keep features within `up-self-biz` unless an existing module has the responsibility already. A new feature does not require a new Maven module. Do not turn this project into microservices or add infrastructure to match a generic architecture checklist.

### 2.2 Backend responsibilities

The usual call sequence is Controller to Service to Mapper to MySQL. Return types move outward through explicit domain or view objects; HTTP handling stays in the web layer.

| Component | Responsibility |
|---|---|
| Controller | Request binding, bean validation, permission annotation, pagination and response wrapping |
| Service interface | Business operations exposed to the controller |
| Service implementation | Role and ownership checks, state transitions, transaction boundaries, concurrency and persistence coordination |
| Mapper interface | Typed persistence operations |
| Mapper XML | SQL, dynamic filters, relationship restrictions, field projections and explicit mappings |
| Domain object | Persistent record representation |
| Request DTO | Accepted input fields and validation constraints |
| View DTO | Response fields needed by that screen or caller |

Use feature packages such as `com.upself.biz.assignment`, `mood`, `goal`, `learning` and `selfefficacy`. Each keeps its own domain, DTO, mapper and service code. Place XML in the matching `up-self-biz/src/main/resources/mapper/` feature directory. Controllers live under `com.upself.web.controller` in the admin module.

Do not put SQL, file persistence or workflow decisions in controllers. Do not let a mapper derive identity from request fields. When an operation depends on the authenticated student, obtain the student ID from the login context in the service.

### 2.3 Names and object design

Use Java class names in PascalCase, fields and methods in camelCase, and constants in UPPER_SNAKE_CASE. Existing service interfaces use the `I...Service` pattern and implementations use `...ServiceImpl`. Preserve local conventions when extending a feature. Mapper method names should describe their action and scope, such as selecting a student's history or updating a record at an expected version.

Use request objects to restrict writable fields. Keep audit timestamps, ownership, calculated scores and security fields server-controlled. Avoid passing a database entity with every column directly into a general update operation.

Dedicated projections are part of the privacy boundary. Mentor mood responses include the journal text and recordings shared with that current mentor; raw storage keys remain private. Mentor observation notes are a separate author-private record. A mentor self-efficacy response must not contain the student's three answers. Omitting a visible component on the frontend does not remove a leaked JSON field.

### 2.4 Controllers and response contracts

Controllers extend `BaseController` in `up-self-common`. Detail and write operations use `AjaxResult`; paginated lists use `TableDataInfo`. Keep existing public field names.

```json
{"code":200,"msg":"Success","data":{"id":123}}
```

```json
{"code":200,"msg":"Success","rows":[],"total":0}
```

Successful login returns `token` at the top level. Some operations return an ID in `data`; do not assume every write returns the full updated entity. An empty optional detail may be serialized as an empty object by the existing response wrapper; feature API adapters normalize it where necessary.

Use `@Valid` on request bodies and the existing validation annotations. Use `@PreAuthorize("@ss.hasPermi('feature:resource:action')")` for method permission checks. The service must still enforce the caller's role, ownership, current course relationship and record status.

### 2.5 Pagination

The public page parameter is `current`, default 1. `pageSize` defaults to 10 and is clamped to 1 through 100. The temporary `pageNum` alias is used only when `current` is absent. Both parameters are integers; malformed values fall back to defaults.

PageHelper applies to the next query on the current thread. Complete any authorization query before calling `startPage()`. After it, call the intended list mapper without an intervening query. Do not convert the paged result into a new ordinary list before `getDataTable()` reads its total.

```java
public TableDataInfo history(Long studentId) {
    service.verifyMentorAccess(studentId);
    startPage();
    try {
        return getDataTable(service.mentorHistory(studentId));
    } finally {
        clearPage();
    }
}
```

This pattern follows the existing controllers. `getDataTable()` uses `new PageInfo<>(list).getTotal()`. The database query supplies the filtered total; the frontend must not replace it with the length of the current page. Keep ordering deterministic. Chapter, resource and post collections are deliberately unpaged small collections and retain `data` arrays.

### 2.6 Errors and logs

`GlobalExceptionHandler` converts `ServiceException` into its existing code and English message. It handles validation errors, malformed input, missing request fields, upload limits and data integrity failures. Unexpected exceptions are logged with a stack trace and return a short availability message.

| Business code | Meaning and client action |
|---|---|
| 200 | Operation succeeded |
| 400 | Invalid input or a persistence constraint; show the supplied message |
| 401 | Missing, expired or invalid login; clear the session and return to login |
| 403 | Access denied or required acknowledgment missing; keep the session |
| 404 | Record unavailable in the permitted scope |
| 409 | A stale version, repeated check-in or changed workflow state; refresh without discarding useful input |
| 429 | Login request limit reached |
| 500 | Unexpected service failure |
| 1010 | Invalid credentials or password lockout |
| 1018 and 1019 | Retained two-step ticket and verification errors; two-step login is currently disabled |

Business codes and HTTP status are distinct in this implementation. The authentication entry point writes HTTP 500 with JSON `code:401`; method access failures commonly use HTTP 200 with JSON `code:403`. The frontend must inspect the JSON response even for a non-2xx HTTP result. Document this behavior rather than silently introducing a different error envelope.

`ServiceException` is unchecked, so the existing transaction rules roll it back where a transaction is active. Handle expected duplicate or version conflicts within the feature where their meaning is known. The generic duplicate-key handler currently has an account-specific message; a new feature should not depend on that wording for an unrelated conflict.

Audit significant actions using the existing audit mapper. Log identifiers and outcomes, not passwords, tokens, media tickets, submitted answers, mood contents or private notes. Keep diagnostic details in the server log and ordinary error messages in English.

### 2.7 SQL and datasource access

Use the existing MyBatis interfaces and XML mappings. Bind values with `#{...}`. Build optional filters with the established XML dynamic SQL tags. Do not interpolate user-provided sort expressions or filters with `${...}`. Select only the fields needed for the caller.

Relationship restrictions belong in SQL as well as service checks where a read can become unauthorized during a concurrent change. Examples include an active mentor assignment, published course and active enrollment. Repeat the scope restriction in the final history query after a pre-pagination permission check.

`DruidConfig` creates the master datasource and the optional slave datasource, then exposes the existing `DynamicDataSource`. The slave is disabled in the current local configuration. The routing annotation and aspect are retained; the MVP does not require a replica or a second database server. Do not add read routing unless an actual configured datasource needs it.

Use the current database field conventions: snake_case column names, explicit mappings and migration-defined indexes. Retain MySQL 5.7-compatible SQL. A MySQL 8 query that happens to work in a developer's unrelated database is not a valid replacement.

### 2.8 Transactions and concurrency

Set transaction boundaries around a complete business state change. Existing assignment writes use READ_COMMITTED so a query after waiting for a row lock sees a just-committed submission. This is scoped to the affected methods; it is not a new global datasource isolation level.

For assignment writes, maintain the existing lock order of course, assignment and student submission. Check optimistic versions after taking the relevant locks. Request keys and unique constraints make repeated formal submissions return the existing attempt instead of inserting another attempt.

Mood and self-efficacy writes serialize against the student row and enforce the interval again inside the transaction. Database uniqueness supplements the application check. Personal goal updates use owner ID and expected version in the same SQL update or delete. Bookmark uniqueness prevents duplicate saved courses.

When files are written with a transaction, register rollback cleanup for newly written files. Remove obsolete draft files only after the database operation commits. A database transaction does not make disk operations atomic. Keep database and file backups together, and do not claim that a background orphan-file cleanup service exists.

### 2.9 Frontend organization and dependencies

The frontend uses Vue single-file components, Composition API and TypeScript. Keep application wiring in `app`, business features in `features`, and reusable primitives in `shared`.

```text
frontend/src/
  app/                 routing, layouts and application wiring
  features/
    auth/              login, identity and profile security
    accounts/          account administration
    learning/          courses, content, progress and bookmarks
    assignments/       drafts, submissions and reviews
    mood/              weekly mood and mentor read views
    self-efficacy/     questionnaire, history and trends
    goals/             private learning goals
    preferences/       reading preferences and speech controls
    dashboard/         role home pages composed from existing APIs
  shared/
    http/              one HTTP client and response contracts
    auth/              session storage and pure permission helpers
    i18n/              language registration and English messages
    styles/            shared tokens and base styles
    ui/                generic feedback, navigation and pagination
```

Pages compose a workflow. Feature API files call the shared HTTP functions and expose typed responses. They do not import pages or layout components. The shared HTTP layer does not import the router or a business store; application wiring registers its unauthorized callback. This keeps login recovery from creating a circular dependency.

Use Pinia for identity and cross-page preferences. Keep a page's form, loading state and temporary upload list within that feature. Extract a composable when it owns a repeated workflow such as draft saving or timeline loading. A generic CRUD engine, schema-driven form platform or store for every response would add maintenance work without helping the present scope.

Reuse small components when behavior and meaning match: pagination, loading/error/empty states, confirmation dialogs, media readers and read-only timelines. Student and management pages may share those components without sharing an entire page. Keep ownership and role decisions out of generic UI components.

Use descriptive PascalCase component names, use-prefixed composables and explicit API operations such as listCourses or assignCourseMentor. Components receive props and emit events rather than mutating parent state. Prefer composition to global mixins or inherited page classes. TypeScript strict checks are part of the existing setup; do not hide uncertain JSON shapes with any. Shared code should have a stable responsibility, not become a dumping ground for unrelated feature helpers.

### 2.10 Requests and screen behavior

`shared/http/client.ts` is the request entry point. It adds the Bearer token, uses `/api`, interprets the existing JSON envelope and exposes data, table and authenticated download helpers. Keep binary downloads on this client so JSON error bodies are not mistaken for files.

Reset pagination to page 1 when filters change. Cancel or ignore stale requests when users change a course, student or time range quickly. Prevent duplicate clicks while a mutation is pending. Preserve a useful form value after a network failure, and show an explicit retry action rather than clearing the form.

Confirm irreversible actions and formal submissions. Offer a clear cancel action. Protect unsaved edits when leaving a page, and release timers, listeners, object URLs and active speech when the component is disposed. An upload finishing does not mean that an assignment was formally submitted.

### 2.11 Inclusive interface rules

Keep student screens in plain English with a clear primary action and predictable navigation. Put the name beside an icon or emoji; an image alone is not a sufficient label. Use text and shape as well as color to show state. Avoid flashing effects, autoplay, unnecessary countdowns, public rankings and penalties for missing a mood entry.

Provide visible keyboard focus, associated form labels, native radio groups, a skip link, readable error feedback and logical focus after navigation. Current reading preferences offer three font sizes, high contrast and reduced motion. They are stored in that browser, not synchronized between devices. The top-right `Aa` action opens Reading settings.

Images need meaningful alternative text. Videos use native controls, optional WebVTT captions and a transcript field. Captions are authored by the mentor, not generated automatically. Text-to-speech uses the browser's available system voices and handles missing or failed speech services. Test actual audio on the target demonstration computer.

Use localized message keys for new visible text. Current UI messages are English, and content has a `languageCode`; neither constitutes a complete language switcher or automatic content translation. Keep backend messages and validation text consistent with the existing English message catalog.

These are implemented accessibility measures and development rules. They are not evidence of a completed study with neurodiverse students or certification against every accessibility requirement.

## 3 Roles authentication and privacy

### 3.1 Public roles and internal maintenance

| Role | Role key | Main responsibility | Main menu |
|---|---|---|---|
| Student | `student` | Learn, submit work and manage personal records | Home, My learning, Assignments, My check-in, My goals |
| Mentor | `mentor` | Teach assigned courses, review work and view permitted student summaries | Home, Teaching courses, Assignments, Students |
| Administrator | `platform_admin` | Manage accounts, courses, mentors and enrollments | Home, Accounts, Course allocation |
| Internal maintenance | `admin` | Restricted internal support through a separate entry | Internal home and permitted administration tools |

The assignable public role IDs are 10 for students, 20 for mentors and 30 for administrators. The application uses one main role per account. Ordinary administrators cannot assign maintenance privileges or create arbitrary permission sets.

Within My learning, students find My courses, Saved courses and Learning history. Within My check-in, they find My mood and Self-belief. Mentor Students contains Student overview and Student check-ins. Reading settings are opened by the top-right Aa control. These groupings organize existing functions without changing their permissions.

Mentors and administrators share reusable management components, but their responsibilities differ. An administrator can allocate courses without gaining access to student submission bodies or private check-ins. Maintenance is absent from the public role selector and uses `/#/internal/login`.

### 3.2 Login and sessions

Student login uses `POST /client/login`. Mentor, administrator and internal maintenance login use `POST /empLogin`. The selected frontend role controls the entry and expected role; the backend validates the actual account role.

Authentication uses the existing Spring Security provider and password handling. The service applies the existing MD5 preprocessing before BCrypt credential matching. Keep this representation consistent with existing stored credentials; do not substitute a different hash format during an unrelated feature change. Password input must travel over HTTPS on a public deployment.

Two-step verification is disabled by `custom.totp.enabled=false`. All current roles sign in with username and password. The retained `/empLogin/totp` endpoint refuses ticket login while disabled. Existing authenticator data remains stored but is not part of the current login flow.

JWTs use HS512 and contain a channel and session identifier. The student and management channels have separate secrets, each at least 32 characters and different from the other. The application reads the authoritative session from Redis and checks the current account status, authorization version and role. Default session lifetime is 120 minutes, refreshed by authenticated activity. These are Redis-backed sessions; a JWT alone does not grant access after its session has been removed.

Logout removes the session. Password changes, administrative resets, account changes and authorization-version changes invalidate affected login state. Redis restart loses local sessions; users sign in again. It does not erase MySQL records.

Login protection uses Redis counters and the existing rate-limit annotation/aspect. Each login route allows 20 requests per IP per 60 seconds. Password failures are counted under a normalized account key. The fifth failure establishes a 30-minute lockout, and attempts beyond the limit are rejected. Avoid treating every authentication error as a wrong password. A role-entry mismatch is reported as an entry error.

### 3.3 Permissions and resource scope

Menu visibility, router guards, method permissions and SQL scope serve different purposes. The first two help users find valid actions. Backend checks enforce access, including requests made outside the UI.

| Resource or action | Student | Current assigned mentor | Administrator | Maintenance |
|---|---|---|---|---|
| Create course and assign mentor or students | No | No | Yes | Existing internal permissions |
| Maintain assigned course content | Read published accessible content | Yes, within assigned courses | Course administration permissions | Existing internal permissions |
| Publish and review assignments | No | Yes, within assigned courses | No | No business-role bypass |
| Student draft and submitted work | Own work | Submitted work only, within current scope | No | No business-role bypass |
| Weekly mood level and history | Own | Read-only after disclosure acknowledgment | No | No business-role bypass |
| Shared mood journal | Own | Current course mentors, read-only | No | No |
| Mentor observation note | No | Author only, while currently responsible | No | No |
| Business ideas and cost plan | Own | No | No | No |
| Self-efficacy score and trend | Own | Read-only after separate acknowledgment | No | No |
| Individual self-efficacy answers | Own | No | No | No |
| Personal goals and bookmarks | Own | No | No | No |
| Student progress writes | Own accessible chapters | No | No | No |

Permission strings follow the current colon-separated style, for example `learning:course:assign`, `assignment:review`, `mood:students:read` and `efficacy:own:record`. Migration scripts assign these to existing roles. Do not create a general permission editor or grant `*:*:*` to ordinary administrators.

The internal role has broad technical permissions, but sensitive services explicitly require the appropriate business role. A wildcard does not override those role checks. Database operators and people holding backup files have a different level of access from application users; UI permissions cannot protect a raw SQL dump from its holder.

### 3.4 Current course relationships

Student content access requires an active enrollment and a published, non-deleted course. Draft chapters and resources remain hidden even inside a published course. A mentor's access depends on the current course assignment, not who taught the course in the past.

Removing an enrollment, changing a mentor or archiving a course affects subsequent authorized reads. If a mentor still has another valid published course with that student, the remaining relationship continues to permit shared check-in access. Service and mapper logic must consider all qualifying relationships rather than blindly revoking access when one enrollment changes.

Check-ins belong to the student globally rather than to one course. Eligible current mentors can read the acknowledged history, including earlier entries. First-use text explains that future course and mentor changes can alter the viewers. There is no per-entry sharing toggle or selective historical sharing feature.

### 3.5 Account administration

Administrators create accounts, edit display names and public roles, enable or disable accounts, and perform permitted password resets. Account usernames use `[A-Za-z0-9_.@-]{3,100}`. Display names are limited to 60 characters. New passwords are 10 through 64 characters. Account status is `0` for active and `1` for disabled.

Usernames are not renamed in this version. Disabling replaces account deletion. Ordinary administrators cannot modify maintenance, reset another administrator's protected credentials, or bypass protection by changing that administrator's role or status. Self-disable and self-demotion are rejected.

Initial account names are `student01`, `mentor01` and `admin01`. Read bootstrap values from `config/application-local.yml`; do not copy actual passwords into this handbook. If a password was changed after initialization, the database credential is authoritative. Bootstrap does not overwrite an existing account's password.

## 4 Feature workflows

### 4.1 Course setup and publication

The administrator opens Course allocation, creates a course, assigns an active mentor, adds students and publishes it. Publishing requires a valid mentor assignment. Courses use DRAFT, PUBLISHED and ARCHIVED states. Archiving preserves data while removing student availability and preventing ordinary teaching writes against the archived course.

Course inputs include title, description, language, difficulty and estimated minutes. The title limit is 150 characters and description limit is 20,000. Difficulty is BEGINNER, INTERMEDIATE or ADVANCED. Estimated time is 1 through 10,080 minutes. Existing courses received default values of Beginner and 30 minutes when V6 was introduced; these values need editorial review rather than being treated as measurements.

Students see courses they have joined through administrator allocation. There is no public course marketplace or student self-enrollment workflow. Title search, pagination and detail access all retain the same scope.

### 4.2 Chapters and teaching resources

An assigned mentor opens a teaching course and adds or edits chapters. Each chapter has a title, plain-text body, order, publication state and content language. Chapter order is 0 through 99,999. Draft content stays hidden from students. Course archive restrictions apply before content changes.

| Resource | Accepted format | Per-file limit | Required content metadata |
|---|---|---|---|
| Image | JPEG, PNG, WebP | 10 MB | Meaningful alternative text |
| Video | MP4 | 100 MB | Title, state and language; optional linked caption and transcript |
| Caption | WebVTT | 1 MB | Same-chapter caption resource with valid publication state |

The backend checks extensions and signatures and rejects SVG. It does not transcode MP4. Use a browser-compatible encoding and test playback, seeking and captions in the target browser. A file being accepted by the upload endpoint does not prove that every device can decode it.

Upload a caption in the same chapter before linking its ID to the video. If a video references a caption, publishing the video requires the caption to be published. A referenced caption cannot be deleted or unpublished while it remains in use. Captions are optional metadata rather than a claim that every uploaded video already has authored subtitles.

Image alt text is limited to 500 characters and transcripts to 20,000. Resource titles are limited to 150. Text is rendered as text, not executable HTML. Content deletion is soft deletion and may retain the underlying disk file; automatic retention cleanup is not implemented.

### 4.3 Private media delivery

The reader obtains a temporary media URL from `POST /resources/{id}/access` under its authenticated learning prefix. The URL contains an opaque ticket, not the login JWT. Tickets are stored in Redis for 1,800 seconds. The backend rechecks the session and live resource access on each ticket request.

Use the shared media URL helper to convert the returned relative address into the frontend `/api` address. Obtain a separate authorized URL for a caption track. Byte-range requests support video seeking and return partial content when appropriate. Direct authenticated content requests remain available for suitable callers.

Ticket URLs are temporary credentials. Do not publish them, put them in persistent course content or log their query context. A ticket does not bypass removal from a course, loss of publication, account disablement or logout. Never expose `.local/uploads` as an unrestricted static directory.

### 4.4 Posts and meeting announcements

Mentors create ANNOUNCEMENT or MEETING posts in their assigned courses. Posts have a title, plain-text body, publication state and language. MEETING requires a supported HTTPS meeting URL; an optional start time uses ISO 8601 with an offset. Links launch the external meeting service.

The server assigns author identity from the login context. A mentor may maintain their own posts in currently assigned courses. A replacement mentor cannot edit the former mentor's posts as if they had authored them. Administrative content controls include taking posts offline; course administration does not mean impersonating a mentor.

Posts do not implement chat, direct messaging, message delivery receipts or an appointment workflow. Keep this distinction in demonstrations and FR reporting.

### 4.5 Learning progress and resume

Opening a chapter saves the student's latest visit. The learner explicitly chooses Mark as complete or Mark as not complete. Opening a chapter or playing a video does not establish completion or understanding.

The course denominator includes only current published, non-deleted chapters. Zero chapters means 0 percent. Adding or withdrawing chapters changes the denominator. An existing chapter's completion is retained when it is temporarily unpublished and later republished.

Continue learning opens the most recently accessible course and chapter. If there is no accessible visit, omit the resume action rather than offering a broken link. Learning history is a paginated list of currently accessible completed chapters. Undoing completion removes that chapter from the visible history; it is not an immutable activity log.

Progress writes are student-only. Mentors cannot mark completion on behalf of a student. Repeated completion does not rewrite its timestamp; undoing and completing again establishes a new completion time.

### 4.6 Course bookmarks

Students save a course from the list or detail page and find it under My learning, Saved courses. Saving requires a currently enrolled, published course. PUT saves and DELETE removes the authenticated student's bookmark. Repeated requests are idempotent.

A bookmark does not grant course access. If the course becomes inaccessible, the saved list returns `available:false` and `course:null`. The student can remove this placeholder without seeing the old course title, description or mentor. Ownership cannot be selected through a request `studentId`.

The list uses server pagination. Bookmarks persist in MySQL across browser changes and logins. They are different from reading preferences, which remain local to the browser.

### 4.7 Assignment publication

The current course mentor creates an assignment with instructions, a maximum score and an optional due date. Assignments use DRAFT, PUBLISHED or ARCHIVED. Titles are limited to 150 characters and instructions to 20,000. Maximum score is an integer from 1 through 1,000, default 100.

Students can read a published assignment in a published course they have joined. Archiving hides the assignment while preserving student work. The current interface supports editing and archiving, not physical assignment deletion. An archived course cannot receive ordinary assignment edits or reviews.

### 4.8 Student draft upload and submission

The student opens an assignment, writes an answer and optionally attaches files. At least text or a file is needed for a formal submission. Save draft is separate from Submit my work. The draft workflow also attempts an automatic save after 30 seconds while editing; it does not automatically submit.

| Submission constraint | Current rule |
|---|---|
| Accepted files | PDF, DOCX, PNG, JPG and UTF-8 TXT |
| Files per attempt | At most 5 |
| File size | At most 20 MB each |
| Answer text | At most 20,000 characters |
| Late work | Allowed, marked as late by the server |
| Resubmission | Available after the mentor returns work for changes |
| Old attempts | Retained; a new attempt does not overwrite them |

The frontend sends the current submission version, assignment version and request key when submitting. The service checks them inside the transaction. A repeated request key returns the same formal attempt. A conflicting version returns 409, and the UI asks the student to refresh rather than silently overwriting another edit.

Draft attachments can be removed by their owner. Once an attempt is submitted, its attachments belong to its history and cannot be overwritten or removed as draft files. New work after a return uses a new draft and, where needed, newly uploaded attachments.

Files use generated storage keys. DOCX handling checks the container and expansion limits and rejects macro content; it does not extract arbitrary archive entries to the server. Downloads require authentication and live ownership or mentor scope. Responses force an attachment download and restrictive content headers.

### 4.9 Mentor review and feedback

The mentor selects the assignment, opens Review work, reads submitted answers and downloads the submitted files. They enter feedback and either grade the work or ask for changes. Feedback is required, with a 10,000-character limit. A grade must fit the maximum score captured with that attempt.

Sending feedback makes it visible immediately. There is no separate review-draft publication stage. A later assignment maximum-score change does not rewrite the historical attempt's maximum. Returned work reopens editing for that student and retains the previous attempt.

Drafts are private to the student, including a new draft after a previous return. The mentor reads formally submitted attempts, not unsent edits. Administrators and maintenance do not acquire review access from their account-management role.

### 4.10 Weekly mood

Students open My check-in, My mood. Before first use, they read a disclosure naming the current viewers and explaining journal and recording access under current mentor relationships. They explicitly acknowledge it or choose Not now. Deferring does not restrict courses or assignments. The acknowledgment is versioned independently from the self-efficacy disclosure.

The service uses `Asia/Kuala_Lumpur`. A week begins on Monday at 00:00 and ends just before the next Monday. The stored key is the local Monday date. The student selects Very low, Low, In between, Good or Very good, with text beside the emoji and no preselected value. The optional journal text is limited to 2,000 characters and shared with current qualifying course mentors.

Saving requires confirmation. A saved entry cannot be edited or deleted in this MVP. The server accepts only the current week, rejects past or future periods, and permits one entry per student per week. Concurrent duplicates return a conflict after one successful write.

History is paginated. Trend ranges are 4, 12, 26 or 52 weeks, default 12. Missing weeks remain No record. The mood graph connects only adjacent recorded weeks, so it does not draw a line across a missing week. A table provides the same information in text.

Current qualifying mentors can read acknowledged mood levels, dates and history. Their response type and SQL projection include the shared journal and permitted recording metadata, without exposing storage keys. Students keep their own history if enrollment changes. Recording is voluntary; the product has no streak penalty, diagnostic labels or automatic risk classification.

### 4.11 General self efficacy

Self-belief sits beside My mood within My check-in, but its data, disclosure, time interval and trend remain separate. The instrument is the English General Self-Efficacy Short Scale-3, identified in the application as `GSE-3-EN-2021-v1`.

The backend provides three original statements and five response options. The interface shows one statement at a time, without a default answer, timer or automatic advancement. Students can move back before saving. All three responses must be integers from 1 through 5.

The server computes an unweighted mean, rounded HALF_UP to two decimal places. Answers 1, 2 and 2 produce 1.67. The server ignores client-supplied ownership or calculated scores. It stores the raw responses and instrument version with the result.

After the first entry, the next entry is available 28 Malaysia calendar days after the previous assessment date, from local midnight on that day. This is an application interval, not a mandatory GSE-3 measurement schedule. Students cannot backfill, edit or delete saved assessments.

Students see their own answers, history and actual-date trend. The UI offers 4, 12, 26 and 52-week views. The graph joins saved observations at their real dates without inventing intermediate scores. This differs from the mood graph's weekly gap handling.

After the separate acknowledgment, current eligible mentors see the assessment date, score, instrument version and trend. They do not receive the individual answers. Administrator and maintenance requests are rejected by business-role checks. There are no mentor write endpoints, combined mood-confidence scores, ranking bands or diagnostic thresholds.

The instrument attribution is Doll, Niessen, Schmidt, Rammstedt and Lechner, 2021. The authoritative source is [GSE-3 English adaptation](https://doi.org/10.6102/zis294), with [instrument documentation](https://access.gesis.org/zis/1435). Scale content is licensed under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/); preserve the attribution, original wording and licensing notice. This does not relicense all project code. The original validation population does not establish validation for Malaysian neurodiverse students.

### 4.12 Personal learning goals

Students create a private goal with a required title of at most 150 characters and optional notes of at most 2,000. Goals are ACTIVE or COMPLETED and do not require a course or deadline. Students can edit, complete, reopen, filter and permanently delete their own goals after confirmation.

Editing a completed goal keeps its state. Repeating completion preserves its original completion time. Reopening clears the completion time. Deletion requires the expected version and removes the goal, not any learning history or course data.

Ownership and version are part of every read or mutation restriction. A mentor, administrator or maintenance account cannot use the private goal APIs, even with broad technical permissions. Missing and inaccessible goals use the same unavailable response to avoid revealing ownership.

### 4.13 Mentor student overview

Student overview contains one row per accessible student-course relationship. It includes published chapter completion, assignment state counts and the latest acknowledged shared mood level and week. It offers search, pagination and links to existing teaching and check-in screens.

A draft or missing assignment submission counts as not yet submitted. Archived assignments are excluded. The latest mood may be from a prior week and must retain its actual week label. The overview does not return submission bodies, attachments, journal text or private mentor observations. It monitors self-reported chapter completion, not every click or second of video viewing.

### 4.14 Extending an existing feature

Start from a confirmed FR and inspect the relevant feature's controller, service, mapper, UI and tests. Define the allowed state change and data scope before adding controls. Add a Flyway migration only when the schema or versioned permission data changes; do not revise an executed migration.

Implement the mapper and service operation, then the controller contract and feature API adapter. Add the page interaction using existing UI feedback, permission helpers and i18n. Verify success, validation errors, stale versions, inaccessible records and another user's requests. Finish by testing the action in the real browser for the relevant roles, with an actual database and private file when applicable.

### 4.15 Private mentor observations (FR-60)

Open Students, Student overview, then Mentor notes on a student row. A note belongs to its author and student, rather than a course, so the same mentor does not duplicate notes across shared courses. Current access requires an active student enrolled in at least one published, non-deleted course assigned to that mentor.

Only the author can read and edit a note while that relationship exists. Another responsible mentor sees only their own notes. Students, administrators and internal maintenance have no content bypass. Reassignment or enrollment removal revokes the former mentor's subsequent access. Records remain in MySQL and are not inherited by a replacement mentor. These observations are separate from student journals, which are shared under the check-in rules.

The form accepts plain text up to 2,000 characters. Create and edit use the existing service layer and transaction conventions. Version checks reject conflicting edits with code 409. Audit rows contain IDs and outcomes, never note content. List responses use BaseController pagination and TableDataInfo.

### 4.16 Business practice (FR-44 and FR-46)

Students open My learning, Business practice. Its two sections are My ideas and Cost calculator; the five main student navigation entries remain unchanged. Home offers a short entry card rather than embedding either form. All copy is English and uses the existing i18n keys, reading preferences and shared form styles.

My ideas stores a required name up to 100 characters, plus optional offer, audience, resources and next-step answers up to 500 characters each. Students can create, reopen and edit their own ideas, including incomplete answers. Foreign IDs return the same unavailable response as missing IDs. Editing requires the current version.

The calculator estimates one batch of items in MYR. Materials cost and other costs refer to that whole batch. Quantity is a whole number from 1 through 1,000,000; price is per item. Money inputs are non-negative with up to ten integer and two fractional digits. Total cost is materials plus other costs. Expected revenue is quantity times unit price. Estimated profit is revenue minus total cost; negative results are explicitly labelled as a loss.

The server uses BigDecimal and returns money as decimal strings, including large totals. The frontend formats these strings without floating-point conversion. Calculate does not persist. Save plan stores one current plan per student, independently of ideas; Reload saved plan restores it. Version zero represents no saved plan, first save creates version one, and stale or concurrent saves fail with code 409. Changing inputs clears the displayed result until another calculation. Students own both their ideas and cost plan; mentors, administrators and maintenance cannot access them.

New database objects and role permissions are in V13__mentor_notes_and_business_practice.sql. The existing Maven modules, dependency versions, authentication channels and startup configurations are unchanged.

### 4.17 User reports (FR-34)

In a course discussion, a student or mentor opens another sender's message menu and selects **Report user**. The form offers four reasons: Hurtful behaviour, Spam, Inappropriate content and Other. Details are optional and limited to 500 characters. Reporting oneself or filing another pending report for the same message is rejected. The server checks current group access and derives the accused user and evidence from the saved message.

**My account > My reports** shows the reporter's own status history. **Administrator > Reports** provides Pending, Resolved and Dismissed filters. The administrator opens a report, reads the saved message and attachment metadata, enters a processing note and resolves or dismisses it. Reporting does not automatically disable an account. Account action remains a separate administrator decision.

Evidence is a snapshot of the reported message, not permission to browse the group or download its files. Reports and processing notes are not shown to the accused user. Reporters see their own status and review date, not the administrator's processing note or other reports. Pending reports use a database uniqueness rule; review uses the record version to reject stale changes.

Source: report controllers in `up-self-admin`, the `report` feature in `up-self-biz`, its Mapper XML and `frontend/src/features/reports`.

### 4.18 Mentor follow-up reminders (FR-58)

The mentor home card and **Students > Follow-up reminders** show situations to review. The student overview links to a student's pending reminders. Only a current assigned mentor can read or review them; students, administrators and internal maintenance cannot access this feature.

| Rule | Trigger | Exclusions |
|---|---|---|
| Low mood | Levels 1 or 2 in two consecutive Malaysia calendar weeks | A missing week breaks the sequence. If this week's entry exists, use this week and the previous week; otherwise use the last two closed weeks. A new non-low entry ends the reminder. |
| Paused learning | Enrollment is at least 14 days old, the course has published chapters still incomplete, and no chapter has been visited for at least 14 days | Recently joined students, courses without published chapters and completed courses are excluded. Enrollment time is the fallback when there is no visit. |
| Overdue work | A published assignment is at least 3 days past its deadline and has never been formally submitted | Saved drafts remain unsubmitted. Returned work already has a submission attempt and is excluded. Undated, draft and archived work is excluded. |

**Mark as reviewed** removes an item from Pending without changing student data. A continuing condition keeps its reviewed state. Once the condition ends and a later episode occurs, the same scoped reminder becomes pending again. Archived courses, changed mentors, inactive enrollment and disabled accounts remove access or end the applicable condition.

Rules refresh daily at 00:05 in `Asia/Kuala_Lumpur`, after application startup, and when the mentor requests the list or summary. The UI provides Pending, Reviewed and Ended views and links to the related course, mood history or assignment and to private mentor notes. These are deterministic follow-up signals, not diagnoses. There is no email, SMS, model inference or missing-mood penalty.

Source: `FollowUpClock`, `FollowUpScheduler`, `FollowUpServiceImpl`, `FollowUpMapper.xml` and `frontend/src/features/follow-ups`. The clock is injected so the weekly and day-based cutoffs can be tested.

### 4.19 CSV exports (FR-54)

**Administrator > Export data** downloads one of three filtered reports. Internal maintenance does not inherit access through its wildcard permission. Each download is an authenticated request and an ordinary local file; there is no background export service.

| CSV | Included fields | Filters |
|---|---|---|
| Users | Username, display name, public role, status and creation time | Keyword, role and status; internal maintenance is excluded |
| Courses | Title, status, assigned mentor, active student count, difficulty and estimated minutes | Keyword and course status |
| Learning summary | Active student-course pairs, published/completed chapter counts and completion percent; assignment totals for not submitted, waiting for review, graded and returned | Keyword and course; only current published courses and active students/enrollment |

Exports exclude credentials, tokens, journals, mood scores, self-efficacy answers, voice recordings, mentor notes, submission content, chats, reports and private business-practice content. Audit details record the export type and row count. Files use UTF-8 with a BOM, quoted CSV cells and spreadsheet-formula protection. The maximum is 10,000 rows; narrow the filters if that limit is exceeded. CSV is a summary report, not a database backup.

Source: `ExportController`, `ExportServiceImpl`, `ExportMapper.xml`, `CsvWriter` and `frontend/src/features/exports`.

### 4.20 Platform reading defaults (FR-55)

The administrator opens **Home > Platform reading defaults** and chooses the starting text size, contrast and reduced-motion setting. The form saves one database record through the existing System module. It does not add a main navigation item. Only the public platform-administrator role can update it; mentors, students and internal maintenance cannot. Stale concurrent saves return 409 and preserve the unsaved form until the administrator reloads the current record.

The app reads the non-personal defaults when it starts and again when Aa opens. The public GET returns only size, contrast, motion and version, with no-store caching. Authenticated writes retain role, permission, validation and audit checks. Failed reads leave the personal controls usable with standard fallbacks and the operating system's reduced-motion preference. Aa provides a retry action.

Personal settings remain browser-based. The v2 browser record saves only fields a person changes. Downloaded defaults are not saved as personal overrides. **Use platform defaults** clears those overrides; an operating-system preference for reduced motion still applies unless the person explicitly chooses otherwise. Existing v1 records are preserved as personal choices because the old format cannot distinguish automatic defaults from deliberate selections. The same reset button lets those browsers follow the new platform defaults.

Source: `AccessibilityController`, `AccessibilityServiceImpl`, `AccessibilityMapper.xml` and `frontend/src/features/preferences`. No generic configuration framework or additional cache is introduced.

### 4.21 Student learning statistics (FR-27)

**My learning > Learning stats** shows current courses, completed/published chapter counts, overall chapter progress and four assignment states. Students can refresh the values and open their courses from the paginated list. The five main student navigation entries remain unchanged. Mentors continue to use the existing student overview rather than another student's private statistics screen; administrators continue to use the permitted CSV summaries.

All figures come from the current student's active enrollment in published, non-deleted courses. Only published, non-deleted chapters and published assignments count. Archived courses, draft content, revoked enrollment and other students' records are excluded. The API derives the student ID from authentication and accepts no target-student selector.

Overall completion is completed chapters divided by published chapters across all included courses, rounded to the nearest whole percent. It is not an average of course percentages. For example, one completed chapter out of four is 25%, even if one small course is already complete. Empty courses add to the course count but not the chapter denominator. A zero denominator shows an explanatory empty state.

Assignment states are Not submitted, Waiting for feedback, Feedback ready and Changes requested. An absent submission or an unsent draft is Not submitted. A returned attempt remains Changes requested. The read model never selects answer text, feedback, scores, journals or assessment answers. Statistics reuse existing progress and submission rows; no activity event stream, watched-minute estimate or mastery score is collected. Reopening a chapter or changing its publication/access state changes the current totals.

Source: the shared `statisticsRows` SQL fragment in `ProgressMapper.xml`, `ProgressServiceImpl`, `LearningStatistics` DTOs and `frontend/src/features/learning/pages/LearningStatisticsPage.vue`. Totals use AjaxResult; paginated courses retain BaseController, PageHelper and TableDataInfo. Text counts accompany native progress bars, and the layout supports the existing Aa settings.

### 4.22 Course discussions, visibility and filtering (FR-28 through FR-35)

Open **Course > Discussion**. The current assigned mentor creates groups and adds or removes enrolled students. Reading requires current course access and active group membership; closed groups keep readable history but reject messages and uploads. Administrators and internal maintenance have no general chat-content access. Reporting remains available through the message menu and administrator report workflow described in section 4.17.

**Hide messages** is a personal setting for one course group. A student opens another student's message menu or Members, confirms Hide messages, and can later choose Show messages in Members. It hides that person's student messages, images, files and voice for that viewer only. The other student is not notified and can still see the viewer's messages. Other groups, group membership and other participants' views are unchanged. Removed hidden members remain in the viewer's Members settings until restored, so the preference can still be cleared.

The service derives the viewer from authentication. SQL excludes hidden messages before the 30-row cursor page is selected, and file endpoints apply the same restriction. Hiding unmounts loaded image and audio components, stops playback and revokes their object URLs. This cannot erase a file someone already downloaded or copied. New messages record the authenticated sender's role at send time. Mentor messages and the current course mentor's messages stay visible. Pre-V16 records did not store this role; V16 labels them LEGACY and keeps them visible rather than guessing from today's account role. The frontend does not decide which role a sender has.

The content filter checks discussion text, attachment display names and image descriptions. The small literal phrase list is [content-filter.txt](../up-self-biz/src/main/resources/chat/content-filter.txt). It normalizes case, compatibility characters, whitespace and punctuation, then matches complete words or phrases. It allows ordinary learning terms, links and requests for help. Both students and mentors use it. A match returns code 422 with a short English explanation; input and pending attachments remain available for correction. There is no automatic account penalty, text replacement or report creation. Staff should review the shipped initial phrases before real student use. It is a limited rule filter, not comprehensive multilingual moderation.

Image pixels, document bodies and recordings are not scanned. Mood journals and assignments are outside this chat filter. Existing report and member-management controls handle material that the literal rules do not catch. Voice remains an uploaded recording, with no transcription, live call or model service.

While a room is visible, one combined **sync** request returns current group status, newer messages and the viewer's hidden IDs every five seconds. Inactive tabs pause; failures retry after fifteen seconds. A page with more new messages is drained in successive short requests. The server captures a committed upper message ID before reading a batch and returns an independent nextCursor, including across hidden-only tails. Message attachment metadata is loaded in one batch query per non-empty result, rather than once per message. Existing message and member indexes are retained; hiding uses a group/viewer/student primary key.

The browser keeps at most 200 loaded message rows. Earlier messages remain in MySQL and load through the existing history button. Reading older history preserves that window and offers **Back to latest messages**; a refresh of history preserves the unsent composer and uploaded drafts. Mutating visibility invalidates in-flight responses, and the next sync detects visibility changes made in another tab.

Message writes retain course-then-group locking and unique client request keys. File validation and disk writing finish before the short database transaction; that transaction rechecks course publication, current mentor or enrollment, membership, group status and the five-draft cap before inserting metadata. Failure removes the new unused file. Each account can run one upload at a time within this single application process. Existing Redis rate limiting permits 60 message-send attempts and 20 upload attempts per user per minute. Limits are per user rather than shared school IP; validation failures and retries count as attempts.

Keep the present monolith and versions. A future push receiver can replace polling behind the feature API, and file storage can move behind the existing private storage service when measured load warrants it. Those changes must retain authorization, cursor ordering, idempotency and private attachment checks. FR-59 mentor encouragement is not part of this delivery.

Source: `ChatController`, `ChatServiceImpl`, `ChatContentFilter`, `ChatMapper.xml`, `frontend/src/features/chat`, and V16. Run `scripts/verify-chat-safety.py --browser` with the local F5 services running; it creates and removes disposable fixtures. `scripts/verify-chat.py --browser` retains the original text/image/file/recording regression workflow.

## 5 Local setup and data migration

### 5.1 Choose the correct startup route

| Computer | Supported workflow | Current verification |
|---|---|---|
| Windows x64 | Existing PowerShell tools, CMD launchers and generated Windows workspace | Real application and browser workflows tested |
| M1 Mac | Separate Bash and Node helpers and generated Mac workspace | Preparation checks passed; actual installation and runtime test pending |
| Linux server | Reviewed production configuration, packaged release, systemd and Nginx templates | Templates prepared; runtime testing paused |

Windows `.exe`, `.cmd`, generated workspaces and MySQL data directories cannot be reused as Mac applications. Source code, Maven configuration, the npm lockfile, SQL logical backups and matching uploads can move between platforms. Do not copy `node_modules`, `target` or a Windows-generated `.local` directory to make a Mac environment.

Use a writable project folder. The original Windows development directory is `C:\Users\eggma\Desktop\codex\up-self`, but a new computer can use another directory. Git access to the repository is required when it is private.

```text
Repository https://github.com/egg48004/UP-SELF.git
Frontend   http://127.0.0.1:5173/
Backend    http://127.0.0.1:8080/
Database   up_self_dev on 127.0.0.1:3306
Redis      127.0.0.1:6379 database 2
```

### 5.2 Configuration locations

`config/application-local.yml` contains the independent project's local database and Redis connection settings, token secrets, disabled TOTP flag, bootstrap passwords and relative upload path. It is currently tracked in the repository. Keep actual values in their configuration files rather than adding another password list to this handbook.

MySQL uses a limited application account for SELECT, INSERT, UPDATE and DELETE, and a separate migration account for schema changes. Root is used only for preparing or restoring the local instance. Existing root passwords are never automatically reset by the project tools.

`learning.storage-path` is `./.local/uploads`, with assignment files under its `assignments` subdirectory. Launchers set the working directory to the project root. If a computer uses another storage path, a matching backup and restoration plan is necessary; changing a path alone does not move files.

Keep local tool paths separate from application credentials. Windows writes `.local/development-tools.json` and `.local/up-self.code-workspace`. Mac writes `.local/macos/tools.json`, `.local/macos/node-path` and `.local/macos/up-self.code-workspace`. These generated machine files are ignored by Git.

### 5.3 Download the fixed tools

| Tool | Official source and selection |
|---|---|
| Oracle JDK 8u231 | [Java archive](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html); choose a JDK for the target OS, not a JRE; complete login and license steps personally |
| Maven 3.10.0 | [Apache Maven archive](https://archive.apache.org/dist/maven/maven-3/); select the exact binary version |
| Node 24.22.1 | [Node release directory](https://nodejs.org/dist/v24.22.1/); Windows x64 or Mac darwin-arm64; verify release checksums |
| MySQL 5.7.44 | [MySQL Community archive](https://downloads.mysql.com/archives/community/); explicitly select 5.7.44 and the target platform |
| Redis Windows 5.0.14.1 | [Windows port release](https://github.com/tporadowski/redis/releases/tag/v5.0.14.1) |
| Redis upstream 5.0.14 | [Upstream release](https://github.com/redis/redis/releases/tag/5.0.14); Mac preparation uses this corresponding upstream version |
| Git and VS Code | [Git downloads](https://git-scm.com/downloads), [VS Code downloads](https://code.visualstudio.com/download) |

Installers are not stored in the repository. If a fixed version is unavailable or fails on the target system, keep the actual error and resolve that specific compatibility issue before proceeding. Do not install a default latest version to make an installer error disappear.

### 5.4 Windows tool configuration

The following example uses `D:\dev` for tools and `D:\projects\UP-SELF` for code. Replace paths with the real installation directories. JavaHome and MavenHome are the directories containing `bin`; NodeHome is the Windows directory containing `node.exe`.

```powershell
git clone https://github.com/egg48004/UP-SELF.git D:\projects\UP-SELF
cd D:\projects\UP-SELF
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure-local.ps1 -JavaHome "D:\dev\jdk1.8.0_231" -MavenHome "D:\dev\apache-maven-3.10.0" -NodeHome "D:\dev\node-v24.22.1-win-x64"
```

The script validates actual Java, Maven, Node and npm versions, saves local paths and generates the Windows workspace. It does not install software, alter system PATH or change a database password. Existing PATH and saved paths can be used when they are correct. Reconfigure after moving the project or tools.

If MySQL and Redis are already running, verify that they are the independent project's intended instances, with the expected versions, ports and passwords. A port being open does not prove service authentication or database identity.

For a new ZIP-based service setup, generate project-local service configuration:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure-services.ps1 -MySqlHome "D:\dev\mysql-5.7.44-winx64" -RedisHome "D:\dev\Redis-x64-5.0.14.1"
```

Only for a completely new data directory, initialize MySQL and use its displayed temporary root password to complete first login:

```powershell
& "D:\dev\mysql-5.7.44-winx64\bin\mysqld.exe" "--defaults-file=$PWD\.local\my.ini" --initialize --console
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\services.ps1
& "D:\dev\mysql-5.7.44-winx64\bin\mysql.exe" -h 127.0.0.1 -u root -p --connect-expired-password
```

Set the new instance's intended root password through MySQL. Do not run initialization or a root password change against an existing data directory. If the new password differs from the snapshot's root credential, supply your own private root client file for the database commands below. The project does not require changing the original computer's root password.

### 5.5 Choose empty initialization or full restoration

These are alternative workflows. An empty initialization creates the schema through application startup and starts without old courses or student records. A full restoration imports existing records and their matching private files. Do not start the backend to create tables and then import a full snapshot over them.

For an empty Windows database:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Init -MySqlHome "D:\dev\mysql-5.7.44-winx64"
```

For a complete snapshot restoration:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Restore -MySqlHome "D:\dev\mysql-5.7.44-winx64"
```

The Windows restore tool validates the snapshot manifest and refuses a non-empty target database or upload directory. Restore the current snapshot together with matching source migrations and referenced files. The manifest records its actual versions and counts; a restore includes only the records present at export time.

Use a read-only check to inspect connection, table count and migration history:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Check -MySqlHome "D:\dev\mysql-5.7.44-winx64"
```

Where needed, add `-RootClientFile "D:\private\mysql-root-client.cnf"`. This file contains a `[client]` section with the new computer's actual user, password, host, port and TCP protocol. Keep it private. Existing application-user passwords are not reset by CREATE USER IF NOT EXISTS; resolve a mismatch instead of repeatedly restoring.

### 5.6 Export the latest Windows database and files

The repository's archived snapshot was created on 22 October 2026 and contains V1 through V5 history. It is not the latest live database. New accounts, goals, bookmarks, assessments and later course changes are not recovered merely by pulling Git or running later migrations.

Before moving the current state, stop application writes by stopping the frontend and backend. Keep MySQL running. Export only the independent database, not the reference project's database.

```powershell
cd C:\Users\eggma\Desktop\codex\up-self
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\database.ps1 -Action Export -MySqlHome "C:\Users\eggma\Documents\Codex\local-env\up-self\mysql"
```

If that MySqlHome differs on the actual machine, use the correct 5.7.44 directory. The export checks that successful migrations match the source, takes a single-transaction logical dump, copies database-referenced learning and assignment files, and writes file sizes and SHA-256 checksums. Missing or changing referenced files cause the export to stop.

After `Snapshot refreshed`, inspect `migration/local-snapshot/manifest.json`. Check its date, database identity and migration versions 1 through 9. The SQL includes every table. The legacy manifest's row-count summary lists only selected tables, so it is not a complete inventory of goals, bookmarks or self-efficacy records.

Move the entire snapshot directory with the matching current application configuration and code. Use a complete Git commit or a private file transfer. Do not copy just `up_self_dev.sql`, manually edit its manifest or combine an older file directory with a new database dump.

After export, Windows development may resume. Any subsequent writes are outside that snapshot. For the latest state at the time of migration, repeat the export at that time.

### 5.7 Windows build and daily startup

From the repository root:

```powershell
.\frontend.cmd install
.\build.cmd
.\frontend.cmd build
.\open-vscode.cmd
```

The frontend install uses the lockfile. Maven uses the existing project-local repository configuration. The first installation needs network access; subsequent offline operation still depends on the required dependencies already being cached.

Open the generated `.local/up-self.code-workspace`, wait for Java import, select `UP-SELF · Backend + Frontend` and press F5. The backend prelaunch task runs the existing service preparation and Maven `process-resources`, ensuring Mapper XML and Flyway SQL reach the output directories before Java loads them.

The alternative terminal workflow is `start.cmd` for the built backend JAR and `frontend.cmd dev` in a second terminal. Build again after Java changes. Do not run these alongside the same F5 processes. Use the Vue address on 5173 and keep MySQL and Redis available.

### 5.8 M1 Mac prerequisites

The planned Mac is an M1 running Ventura 13.2.1. Node 24 official binaries require macOS 13.5 or later. Back up the computer and update macOS to a supported version before installing the fixed Node version. [Node platform requirements](https://nodejs.org/en/blog/migrations/v22-to-v24)

Oracle's 8u231 archive provides a Mac x64 JDK rather than an ARM64 build. MySQL 5.7.44's Mac package is also x86_64. These require a Rosetta compatibility route and actual verification on the chosen macOS release. Rosetta supports running Intel Mac applications on Apple silicon, but it does not prove compatibility of these specific older installers. [Apple Rosetta guidance](https://support.apple.com/en-mide/102527), [MySQL Mac installation guidance](https://dev.mysql.com/doc/mysql-macos-excerpt/5.7/en/macos-installation-pkg.html)

Use the ARM64 Node package. Maven can run through the selected JDK. The Mac Redis baseline is upstream 5.0.14 because the Windows port's release documentation identifies that upstream version. This platform mapping does not claim identical binaries or a completed compatibility test. [Windows port source](https://github.com/tporadowski/redis/blob/v5.0.14.1/README.md)

For Redis source compilation, install Apple's command-line tools, unpack the exact upstream source and use a private prefix:

```bash
xcode-select --install
# After command-line tool installation, enter the Redis 5.0.14 source folder.
make
make install PREFIX="$HOME/Developer/up-self-tools/redis-5.0.14"
"$HOME/Developer/up-self-tools/redis-5.0.14/bin/redis-server" --version
```

Do not replace a failing build with a default latest Homebrew formula. Keep the actual compiler error and resolve it separately. [Redis source build instructions](https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/install-redis-from-source/)

### 5.9 Configure the Mac workspace

```bash
mkdir -p "$HOME/Projects"
cd "$HOME/Projects"
git clone https://github.com/egg48004/UP-SELF.git
cd UP-SELF
bash scripts/macos/up-self.sh configure \
  --java-home "/Library/Java/JavaVirtualMachines/jdk1.8.0_231.jdk/Contents/Home" \
  --maven-home "$HOME/Developer/up-self-tools/apache-maven-3.10.0" \
  --node-home "$HOME/Developer/up-self-tools/node-v24.22.1-darwin-arm64" \
  --mysql-home "/usr/local/mysql" \
  --redis-home "$HOME/Developer/up-self-tools/redis-5.0.14"
```

These are example installation directories. Pass the actual absolute directories on the Mac. NodeHome contains `bin/node`; JavaHome ends in the JDK's `Contents/Home`. The configure command validates versions before writing Mac-only paths and a workspace under `.local/macos`. It leaves the Windows configuration and system PATH alone.

Use the generated workspace for daily work, not the root template. Reconfigure after moving a tool or project directory. VS Code's Java language server may use a newer runtime supplied or supported by the extension; the application remains on JavaSE-1.8. Do not set the language server itself to an unsupported Java 8 runtime. [VS Code Java runtime configuration](https://code.visualstudio.com/docs/java/java-project)

### 5.10 Prepare local Mac services

Start MySQL through the installed package's service controls. Verify that the server on 3306 is the intended 5.7.44 instance. Prepare `.local/macos/mysql-root.cnf` with this Mac's actual root credential:

```ini
[client]
user=root
password="REPLACE_WITH_THIS_MAC_ROOT_PASSWORD"
host=127.0.0.1
port=3306
protocol=tcp
```

```bash
chmod 600 .local/macos/mysql-root.cnf
bash scripts/macos/up-self.sh db-check \
  --root-client-file "$PWD/.local/macos/mysql-root.cnf"
```

The check reads the actual server version, table count and migration history. It does not reset root. If MySQL already exists for another project, do not initialize or overwrite its physical data directory.

Create `.local/macos/redis.conf` with the actual password from the application's local configuration:

```conf
bind 127.0.0.1
protected-mode yes
port 6379
databases 16
daemonize no
save ""
appendonly no
requirepass "REPLACE_WITH_THE_APPLICATION_REDIS_PASSWORD"
```

```bash
chmod 600 .local/macos/redis.conf
bash scripts/macos/up-self.sh redis
```

Leave that foreground terminal open. This local Redis arrangement discards session persistence; a restart requires login again. It does not delete database records. If the correct Redis service is already running, do not start a second one. The application still uses database 2.

### 5.11 Restore or initialize the Mac database

Check that the snapshot application and migration credentials match `config/application-local.yml`. The default private upload directory is relative to the project root. Do not copy Windows absolute tool paths.

For the latest Windows data, export and transfer the complete current snapshot first, then run this command against an empty target database and empty upload directory:

```bash
bash scripts/macos/up-self.sh db-restore \
  --root-client-file "$PWD/.local/macos/mysql-root.cnf"
```

Add `--snapshot "/absolute/path/local-snapshot"` if the snapshot is on an external drive. Mac restore requires the snapshot's migration versions to match the source exactly. It deliberately rejects the repository's archived V5 snapshot, rather than presenting it as a current data migration.

The tool checks manifest paths, file sizes, hashes, upload count, server version and target emptiness. It creates the separate scoped database users without resetting existing passwords, imports SQL, checks migration history and recorded row-count summaries, then copies and verifies files. Root credentials are passed through the private client file rather than written into the command line.

Restoration is not atomic across MySQL and the filesystem. If it fails, keep the error and partial target for investigation. Do not repeatedly import into a partially restored database or delete Flyway history to force another attempt. Prepare a fresh independent target after determining the cause.

For an intentionally empty development database, choose this command instead:

```bash
bash scripts/macos/up-self.sh db-init \
  --root-client-file "$PWD/.local/macos/mysql-root.cnf"
```

It creates the empty database and scoped users. The backend then applies V1 through V13 and runs existing bootstrap logic for an empty user table. It does not restore old records. Do not run both alternatives.

### 5.12 Build and start on Mac

```bash
bash scripts/macos/up-self.sh check
bash scripts/macos/up-self.sh install
bash scripts/macos/up-self.sh build
bash scripts/macos/up-self.sh open-vscode
```

Install runs `npm ci`. Build runs Maven package, including the backend tests, and the frontend type check and production build. Open the generated `.local/macos/up-self.code-workspace`, wait for Java import, then choose `UP-SELF · Mac Backend + Frontend` and press F5, or Fn+F5 where required.

The Mac prelaunch task checks the fixed tools and local service ports before running Maven `process-resources`. It does not install or take over a MySQL system service. A reachable port confirms a listener, not its credentials; inspect Java startup for authentication and migration failures.

For terminal startup, run the following in two separate terminals from the project root:

```bash
# Terminal one
bash scripts/macos/up-self.sh backend
```

```bash
# Terminal two
bash scripts/macos/up-self.sh frontend
```

The backend command uses the built JAR. Rebuild after Java changes. Open `http://127.0.0.1:5173/`. Stop foreground commands with Control+C or stop F5 debugging. Do not start a second copy on the same ports.

The optional Finder launcher is `open-vscode-macos.command`. Grant its executable bit with `chmod +x open-vscode-macos.command` if needed. It opens the already-configured workspace; it does not install software, restore data or start every service automatically.

### 5.13 Flyway rules

Flyway runs as part of Java backend startup. It checks recorded migrations on each startup and executes missing migrations once. Starting Vue does not run database migrations.

| Script | Main change |
|---|---|
| `V1__auth_schema.sql` | Authentication and administration schema |
| `V2__auth_roles_menus.sql` | Initial roles, menus and permissions |
| `V3__learning_and_english_permissions.sql` | Learning schema and English permissions |
| `V4__assignments.sql` | Assignments, submissions, attempts and files |
| `V5__weekly_mood.sql` | Mood acknowledgment and weekly entries |
| `V6__learning_progress.sql` | Course facts and chapter progress |
| `V7__personal_goals.sql` | Personal learning goals |
| `V8__course_bookmarks.sql` | Student bookmarks |
| `V9__general_self_efficacy.sql` | Assessment acknowledgment, records and permissions |
| `V10__knowledge_check_and_shared_diary.sql` | Mentor questions and shared student journals |
| `V11__recordings_and_all_diaries_shared.sql` | Recording metadata and journal sharing policy |
| `V12__course_group_chat.sql` | Course discussion groups, membership, messages and files |
| `V13__mentor_notes_and_business_practice.sql` | Private mentor notes, student ideas and cost plans |
| `V14__reports_follow_ups_and_exports.sql` | Private user reports, mentor reminder state and export permissions |
| `V15__accessibility_defaults_and_learning_statistics.sql` | One platform-default record and settings/statistics permissions |
| `V16__chat_visibility_and_content_filter.sql` | Message role-at-send, per-viewer course-group hiding and student hide permission |

Scripts are in `up-self-admin/src/main/resources/db/draft`. Current configuration enables Flyway, validation and baseline-on-migrate, permits out-of-order migrations and disables placeholder replacement. Migration credentials are separate from application DML credentials.

Add the next migration for a new change; the next integer after V16 is V17. Use `V<number>__<description>.sql`. Never modify an executed migration to adjust an existing installation. Do not hand-create partial business tables before first startup, because baseline behavior can change which versions run.

An empty database receives V1 through V16. An existing V15 database receives V16. Restoring a valid older snapshot through a supported route preserves its history and allows missing migrations to run, but does not recover later business data. A full current restore should not rerun the schema scripts.

On failure, preserve the error, database identity and `flyway_schema_history`. Stop before making further changes. Investigate the failed SQL and migration state before considering a controlled recovery. Do not use clean, blind repair or history deletion as a routine startup fix.

### 5.14 Updating another development computer

Save local changes and back up its database and matching uploads. Stop the frontend and backend, pull the intended commit with `git pull --ff-only`, restore locked frontend dependencies, rebuild and restart. Keep that computer's current database and uploads; let Flyway apply only missing versions.

On Windows, use the existing install, build and open-vscode commands. On Mac, use the corresponding Mac install, build and open-vscode commands. Reconfigure only when tool or project paths change. Log out and in after a permission migration to refresh the visible navigation.

Pulling code is not a request to restore an old snapshot. If both computers have independently created records, a full SQL import does not merge them safely. Use one shared deployed backend for ongoing shared live data.

### 5.15 Troubleshooting

| Symptom | Check and action |
|---|---|
| Wrong or missing tool | Re-run the platform configure command with the actual fixed-version directory |
| Mac Node fails on 13.2.1 | Upgrade macOS into the Node 24 support range before configuring |
| Bad CPU type or old installer failure | Check package architecture and Rosetta; preserve the error for target-machine investigation |
| MySQL Access denied | Check the intended instance, private root client file, app/migration users and matching application settings |
| Redis NOAUTH or WRONGPASS | Check the active service password and application's database 2 settings |
| F5 cannot find a task | Open the generated workspace for the correct OS; do not bypass the required preparation task |
| New endpoint reports unavailable after F5 | Check copied Mapper XML and SQL resources, stop Java and restart after resource preparation |
| Ports already occupied | Identify the running instance before stopping anything; run one frontend and one backend |
| Courses exist but media fails | Restore matching uploads, check live enrollment/publication and refresh the media ticket |
| Assignments appear but attachments fail | Check the matching uploads/assignments directory and authorization |
| Restore refuses the target | Keep existing data and use a fresh independent target; do not remove tables to evade the check |
| Mac restore reports old snapshot | Export current V9 data and files on Windows; do not change manifest metadata manually |
| New permissions are absent | Confirm migration success, restart the latest backend and log out and in |
| Git dubious ownership on Windows | Use the project's existing push helper, which trusts only this path for its commands |
| No text-to-speech audio | Check target browser and installed English voices; text remains available and errors must be visible |

## 6 Deployment and operating costs

### 6.1 Deployment status and intended layout

No server has been purchased and no public deployment has been completed. The attempted WSL2 setup on the current cloud Windows computer failed because the host did not provide the required virtualization. Linux testing is paused until a suitable physical computer or an existing Linux environment is available. This is separate from the M1 Mac local-development preparation.

The intended first release uses one Linux x86_64 server near the Malaysian users, initially comparing Singapore options. It runs Nginx, the current Java application, MySQL 5.7.44, a reviewed Redis 5.0.14 build and private upload storage. It does not need a desktop, microservices, a managed database or an application object-storage service to begin.

Build the Java JAR and Vue production files before transferring a release. The server serves `frontend/dist`; it does not run the Vite development server. Node and Maven are build tools, not required runtime services for the packaged release.

### 6.2 Price basis and limits

Public prices below were checked on 25 October 2026. They are planning figures, not a final checkout quote. Region availability, account eligibility, tax treatment, payment fees and exchange rates must be recorded at purchase. A 10-to-30-student account list does not establish how many simultaneous video streams a server supports.

For comparable MYR estimates, retain the explicit planning assumptions of USD 1 = RM4.10, an 8 percent tax allowance on the monthly subtotal, and 3 percent payment/exchange allowance. These are calculation assumptions, not a current exchange-rate quote or a claim that every service has identical tax treatment. DigitalOcean's Malaysia tax page describes its current customer treatment; other invoices must be checked independently. [Malaysia tax information](https://docs.digitalocean.com/platform/billing/taxes/mys/)

The formula is `monthly USD subtotal x 1.08 x 4.10 x 1.03`. Reserve RM55 annually for a normal domain. Annual domain cash is paid up front, even when comparing a monthly equivalent. Avoid double-counting fees already included in a bank's settlement exchange rate.

### 6.3 Complete small deployment options

| Option and region | Server specification | Server USD per month | Backup allowance USD per month | Monthly subtotal USD |
|---|---|---:|---:|---:|
| A Alibaba Cloud Singapore trial | 1 CPU, 2 GB, 40 GB, 1.5 TB transfer, 6 Mbps peak | 9.00 | 0.15 external backup example | 9.15 |
| B Alibaba Cloud Singapore pilot | 2 CPU, 4 GB, 80 GB, 4 TB transfer, 15 Mbps peak | 19.00 | 0.15 external backup example | 19.15 |
| C DigitalOcean Singapore | 2 vCPU, 4 GiB, 80 GiB, 4,000 GiB transfer | 24.00 | 7.20 daily percentage plan | 31.20 |
| D AWS Lightsail Singapore | Linux with public IPv4, 2 vCPU, 4 GB, 80 GB, 4 TB transfer | 24.00 | 4.00 snapshot budget | 28.00 |

Sources: [Alibaba Cloud plans](https://www.alibabacloud.com/en/product/swas/pricing), [DigitalOcean plans and backups](https://www.digitalocean.com/pricing/droplets), [Lightsail plans and snapshots](https://aws.amazon.com/lightsail/pricing/).

Alibaba publishes both the transfer allowance and a peak bandwidth limit. Option A is the lowest advertised complete example here, suitable for a limited trial subject to testing. Option B provides more memory for a small student pilot. Neither is a promise of the cheapest available checkout worldwide. At an illustrative 2 Mbps per stream, a 15 Mbps peak limit leaves little room for many concurrent viewers after other requests; measure actual videos before inviting a full concurrent class.

DigitalOcean's percentage-based daily backup plan is 30 percent of the Droplet fee, giving USD7.20 on USD24. AWS snapshots are billed at USD0.05 per GB-month; USD4 is a budget based on roughly 80 GB of billable snapshots, not a fixed package or a price ceiling.

The Alibaba examples use an external backup allowance rather than purchasing a second whole-machine backup product. At 20 GB-month of total retained Cloudflare R2 Standard data, 10 GB-month free leaves USD0.15 in storage, assuming requests remain within the applicable free allowances. This is backup storage only, not an application storage integration. [R2 rates and rounding](https://developers.cloudflare.com/r2/pricing/)

| Option | Monthly estimate with tax and payment allowances | Monthly equivalent with RM55 annual domain | First month plus annual domain | Suggested cash reserve |
|---|---:|---:|---:|---|
| A | RM41.73 | RM46.32 | RM96.73 | RM120 initially; RM60 monthly |
| B | RM87.34 | RM91.92 | RM142.34 | RM180 initially; RM120 monthly |
| C | RM142.30 | RM146.88 | RM197.30 | RM240 initially; RM180 monthly |
| D | RM127.70 | RM132.29 | RM182.70 | RM225 initially; RM165 monthly |

Reserves are unspent margin, not extra mandatory charges. Using option B's assumptions, six months of service and one annual domain payment total about RM579.04 before additional usage. A RM800 planning allocation leaves room for actual checkout differences and testing. These are unchanged planning inputs, recalculated rather than presented as amounts already paid.

Additional providers remain available for a later quote. Compare [Akamai Cloud pricing](https://www.akamai.com/cloud/pricing) and [Vultr pricing](https://www.vultr.com/pricing/) against the same region, IPv4, disk, backup and transfer conditions. No new checkout price for these alternatives is asserted here; do not reuse an old promotional figure as a current quote.

### 6.4 Domain and small charges

An ordinary non-premium `.com` from Porkbun is advertised at USD11.08 for registration and renewal at the checked date. Reserve RM55 per year, then verify the selected name, final tax, currency and renewal price. [Domain price table](https://porkbun.com/products/domains)

Basic registrar DNS and a [Let's Encrypt certificate](https://letsencrypt.org/about/) do not require a separate paid subscription. The operator still needs to configure DNS and certificate renewal. A domain purchase does not buy a server, deploy Java or bind the application automatically.

Before paying, account for the following rather than adding every optional product to the cart:

- Provider tax, currency conversion, card cross-border fees or minimum account funding, according to the actual account and payment method.
- Domain renewal, restoration after expiry, premium-name pricing and any paid email add-on. Email is not required by the current password-login MVP.
- Snapshot growth, backup retention, external-storage requests and restore-test transfers. Count all retained copies, not only the size of the live uploads.
- Outbound transfer beyond the plan, extra disks, orphaned volumes or snapshots, and unassigned public IP charges where applicable.
- A short-lived test server or overlapping billing during a migration, if actually used.
- Software licensing for the exact JDK and target use. Do not assume that an archive download is free for every deployment purpose.
- One-time setup labor and ongoing maintenance time. Self-deployment can have RM0 outsourced labor, but someone still performs the work. Obtain a separate quote if hiring support.
- Caption writing, image descriptions, course materials and licensed media. Self-authored content may avoid an external bill; paid production or assets need a separate quote.
- Any school-required review, privacy documentation or external advice. Use the actual requirement and quote rather than inventing a universal approval fee.

CDN, video-on-demand services, application object storage, managed databases, load balancers, paid monitoring, CI/CD hosting and Kubernetes are not initial purchases. Reassess only when measured demand or a confirmed requirement justifies them.

For later comparison, [Backblaze B2](https://www.backblaze.com/cloud-storage/pricing) is an alternative backup supplier and [Alibaba OSS](https://www.alibabacloud.com/en/product/oss/pricing) is an optional storage product, not an enabled application dependency. [Namecheap](https://www.namecheap.com/domains/registration/gtld/com/) and [Cloudflare Registrar](https://www.cloudflare.com/products/registrar/) are alternative domain registrars. Requote their current registration, renewal, request, recovery and transfer charges before substituting them. Do not carry an expired introductory discount into recurring costs.

### 6.5 Storage transfer and bandwidth

Storage is the amount retained on disk. Transfer is the amount sent across the network. Bandwidth is the rate at which that transfer can happen. Local uploads do not create the same provider bill as serving those files from a cloud server to students.

A plan's included transfer can cover video playback. There is no separate mandatory payment every time a student presses Play. Extra usage becomes billable when the plan's rules say the allowance has been exceeded. Ten students downloading a 100 MB file once use about 1 GB before protocol overhead and repeats; replays and range requests also transfer bytes.

Alibaba's current public overage table lists Singapore at USD0.081 per GB and Kuala Lumpur at USD0.077 per GB. Other providers apply their own incoming/outgoing, allowance and regional rules. Check the selected plan rather than assuming every advertised TB has the same meaning. [Alibaba overage rates](https://www.alibabacloud.com/en/product/swas/pricing), [DigitalOcean transfer billing](https://docs.digitalocean.com/platform/billing/bandwidth/), [Lightsail transfer rules](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-faq-data-transfer-allowance.html)

### 6.6 Purchase and binding order

1. Complete a reviewed Linux compatibility test for the fixed JDK, MySQL and Redis builds, and prepare a release and recoverable data backup.
2. Open the chosen provider account and select the intended region, Linux x86_64 image, specification and billing term. Compare the actual checkout total with the budget. There is no project requirement to prepay six months.
3. Purchase the server only after recording the payment checklist in Section 6.10. Set up operator access and firewall rules. Students will not need shell access.
4. Install the reviewed runtime services, prepare the independent production database and configure secrets outside the release directory. Keep MySQL and Redis private rather than opening their ports to the Internet.
5. Upload and verify the program release and matching data. Test the application with non-sensitive test data through the temporary server address.
6. Choose and purchase an ordinary domain after checking availability and renewal price. Basic DNS is sufficient.
7. Add an A record for the root name pointing to the server's public IPv4 address. Add `www` as a CNAME to the root name if that hostname will be served. Remove conflicting records only after confirming their current use.
8. Set the Nginx server names to the chosen domain, wait for correct DNS resolution and issue the certificate. Configure HTTP-to-HTTPS redirection and verify renewal.
9. Configure and test consistent off-host backups, restore once into an independent target, and run the real role workflows over HTTPS before inviting students.

The domain registrar may differ from the server provider. DNS joins the name to the server IP; Nginx joins the incoming hostname to the application. HTTPS adds authenticated encrypted transport. All three need correct configuration.

If Cloudflare DNS is selected, DNS-only records are sufficient for this layout. Enabling its proxy introduces separate plan limits. The published Free/Pro upload limit is 100 MB, which can conflict with this application's 100 MB file plus multipart overhead and 105 MB request allowance. Test the real route before enabling a proxy; buying an upgrade is not the default solution. [Cloudflare request-size limits](https://developers.cloudflare.com/support/troubleshooting/http-status-codes/4xx-client-error/error-413/)

### 6.7 Release files and persistent directories

The existing Windows packager builds or uses the fixed-version outputs and writes a timestamped release under `.local/linux-release`:

```powershell
.\scripts\package-linux.ps1
```

The archive contains `app/up-self-admin.jar`, `web/`, `linux/`, `release.json` and `SHA256SUMS`, with an outer checksum file. It deliberately excludes local credentials, database dumps, uploads and node_modules. A release archive is not a complete data backup. A historical package marked V6 must not be mistaken for a newly built V9 release.

| Proposed production path | Content |
|---|---|
| `/opt/up-self/releases/<release-id>/` | One immutable program release |
| `/opt/up-self/current` | Link to the active release |
| `/etc/up-self/` | Protected production configuration and service environment |
| `/var/lib/up-self/uploads/` | Persistent private learning and assignment files |
| `/var/backups/up-self/` | Local encrypted backup staging, followed by off-host transfer |

The service template runs Java as the `upself` user. `run.sh` uses the selected JDK, prod profile, external configuration, UTF-8 and a 256 MB initial/1,024 MB maximum heap. This heap setting is not evidence that every 2 GB host has enough total memory for Java, MySQL, Redis and the OS.

`up-self.env.example` leaves `UPSELF_LINUX_RUNTIME_REVIEWED=no`. The preflight intentionally blocks an unreviewed runtime. Confirm JDK licensing, target system libraries, MySQL server compatibility and Redis behavior before setting the flag. Do not bypass it just to make a template start.

### 6.8 Nginx and public access

Copy the example templates to their intended system locations and replace every example hostname and placeholder. Test the actual Nginx and service configuration on the target before enabling it.

The HTTPS template serves built Vue files from the current release and forwards `/api/` to `127.0.0.1:8080/`. Keep the trailing-slash prefix behavior: Java receives `/client/...` or `/learning/...`, not `/api/client/...`. Preserve the Authorization header, JSON business errors and Range media behavior.

The template permits a 105 MB request body to match the application's 100 MB file and 105 MB multipart limits. It disables API caching and API access logs that could retain media tickets, and does not expose uploads through a static alias. Its frontend fallback serves index.html for application navigation.

Use normal firewall boundaries: public HTTP/HTTPS, operator-restricted SSH, and private backend/database/cache listeners. Do not host the public site by leaving Vite open on a laptop. A cloud VM normally runs continuously; closing the operator's terminal should not stop a correctly configured systemd service.

### 6.9 Backups updates and rollback

Back up the database and matching uploads from a quiet application state. The existing Linux `backup.sh` requires the application service to be installed and fully inactive. It does not stop or restart the service for the operator. MySQL remains running for the logical export.

The script checks MySQL 5.7.44, exports `up_self_prod`, calculates a SQL checksum, and streams SQL plus `/var/lib/up-self/uploads` into an AES-256-CBC archive using PBKDF2 with 200,000 iterations. It requires private client credentials and an encryption key file, writes an encrypted archive and external checksum, and deletes only its own temporary working folder. It does not delete older backups or upload them off-host.

Copy the encrypted archive and checksum outside the server. Keep the encryption key separately; an encrypted backup without its key is not recoverable. A proposed initial retention is seven daily copies, with actual storage growth measured. Retention and scheduling are operational work still to be configured, not an existing automated service.

For a restore drill, verify the encrypted checksum, decrypt in a protected workspace, verify the SQL checksum, create an empty independent target and restore SQL with its Flyway history and matching file tree. Check row counts, media and assignment downloads, then role permissions. Do not test restoration by overwriting the live student database.

For a feature release, test locally, build the intended commit, create a consistent backup, extract to a new release directory, switch the current link and restart Java during a maintenance window. Flyway applies the new migrations. Smoke-test the public HTTPS workflow and retain the previous release and backup.

Application rollback and database rollback differ. Returning to an old JAR may be unsafe after an incompatible schema change. Review the migration before switching back. Restoring an older database also loses later writes, so the chosen recovery point must be explicit.

### 6.10 Final payment and release checklist

Record one complete list before any actual purchase:

| Item | Required value before payment |
|---|---|
| Provider and account | Product, billing country and owner |
| Region and specification | Location, architecture, CPU, memory, disk, transfer, peak bandwidth and IPv4 |
| Checkout | Actual currency, base price, tax, promotion expiry, renewal and total due |
| Billing term | Monthly, prepaid duration or usage rate; cancellation and retained-resource costs |
| Backup | Frequency, retention, off-host location, expected total size, encryption key holder and restore test |
| Domain | Exact name, initial price, renewal, registrar, expiry reminders and DNS records |
| Payment | Accepted card/channel, settlement currency, applicable bank fees and any temporary authorization |
| Runtime | Actual OS, JDK vendor/build/license, MySQL server, Redis upstream build and supporting service versions |
| Labor | Self-managed work or an explicit one-time support quote |
| Acceptance | HTTPS, three roles, current data, real media, private files, backup recovery and update procedure |

## 7 Verification and demonstration

### 7.1 Recorded evidence and its scope

The V16 delivery was verified on 30 October 2026: 172 Java tests and 111 frontend tests passed, along with TypeScript and the production build. ESLint reported zero errors and the same 48 existing warnings. The chat-safety specialist run passed 125 checks including its real Chrome three-role scenario. The original discussion regression passed 85 API checks and its Chrome scenario. Coverage includes text/image/file/recording exchange, rejected wording with retained drafts, role and membership revocation, course-group hide/show, hidden attachment denial, protected mentor and legacy messages, cursor backlog, bounded history, initial connection failure and retry, and per-user limits. A separate Chrome check verified personal Aa high contrast, extra-large text and a 320-pixel layout.

A local Windows smoke check ran 30 polling clients for three cycles (90 successful requests), alongside course, statistics and assignment reads and a 1 MB upload. The recorded chat median was 14.0 ms and the 95th percentile was 71.6 ms. This is a local concurrency check, not a Hostinger/VPS capacity result. Disposable users, courses and files were removed. The existing VS Code F5 compound started the two services with Java Ready and the three existing workspace warnings. Dependency manifests, startup configuration, earlier migrations and all 1,947 reference-project files were unchanged.

The V15 delivery was verified on 29 October 2026: 158 Java tests, 103 frontend tests, TypeScript and production builds passed. ESLint reported zero errors and 48 existing warnings. The local specialist run passed 167 checks including its Chrome scenario, covering default persistence, stale and concurrent writes, role denial, weighted statistics, publication/enrollment changes, paging, personal overrides, recovery from a defaults outage and mobile layout. In Chrome, marking a real chapter complete changed the displayed totals from 2/4 (50%) to 3/4 (75%). Disposable records were removed and the prior platform values restored. The role-navigation Chrome regression also passed, including keyboard access, the same five main student links, current mentor/admin menus and 320/390/768-pixel layouts with extra-large text. The services were started through the existing VS Code F5 compound.

The V14 delivery was verified on 29 October 2026: 149 Java tests, 94 frontend tests, TypeScript and production builds passed. The specialist suite passed 125 live API checks and a Chrome workflow covering student and mentor reporting, administrator review, current-mentor reminders, three CSV downloads and wrong-role access. It uses disposable fixtures and removes only its own records and uploaded file. VS Code's combined F5 configuration was used for the running services. These results apply to Windows; they do not establish Mac or Linux runtime success.

The earlier feature delivery recorded the following results on the existing Windows environment. These are dated verification records, not a claim that all tests were rerun during handbook consolidation.

| Evidence recorded on 23 October 2026 | Result |
|---|---|
| Java package and tests | 86 passed, including 14 self-efficacy tests |
| Frontend unit and component tests | 40 passed |
| Type check and production build | Passed |
| ESLint | 0 errors and 51 existing template-format warnings |
| Self-efficacy real API suite | 91 checks passed, including isolated fixture preparation |
| Personal-goal specialist regression | 61 API checks plus Chrome workflow passed |
| Bookmark specialist regression | 117 API checks plus Chrome workflow passed |
| Role and core workflow regression | Six core browser workflow groups plus the specialist goal, bookmark and self-efficacy groups |
| Windows VS Code F5 | User restarted the combined configuration; the resulting 8080/5173 instance passed feature/API checks |

Do not add every historical test count together. Earlier deliveries ran overlapping suites against different schema versions. A skipped browser scenario without its specialist fixtures is not a passed test.

Mac preparation added 15 checks on 25 October for version boundaries, workspace generation and snapshot safeguards. Bash and Node syntax checks passed. This is not a Mac MySQL restore, Rosetta compatibility, F5 or browser result. Linux template checks likewise do not establish Linux integration success.

Verification outputs live under ignored `.local/*-verification` directories. Keep useful dated evidence privately rather than adding ordinary logs and temporary result files to docs. Run a suite again when a code or environment change creates a reason to do so.

### 7.2 Reproducing checks

Use the platform's fixed tools to build Java and the frontend. Frontend scripts support typecheck, lint and test. The existing specialist integration scripts require Python, Chrome, a running application where specified and their configured local tool paths; several retain Windows-specific paths and are not ordinary startup dependencies.

```powershell
# With the intended local services running and specialist tools configured:
python scripts/verify-goals.py --browser
python scripts/verify-bookmarks.py --browser
python scripts/verify-self-efficacy.py --browser
python scripts/verify-support.py --browser
python scripts/verify-settings-stats.py --browser
```

The general frontend verification script covers common application flows. Specialist suites create their own isolated accounts and course relationships and clean up only their recorded fixtures. Review each script's setup before running it on a different machine. Do not point verification suites at a live student deployment.

The Mac preparation tests require Node only and do not access MySQL:

```bash
node --test scripts/macos/verify-preparation.test.mjs
```

### 7.3 Minimum role and boundary checks

For each feature, verify a permitted action, a wrong-role action and another user's record. Include a changed relationship: revoke enrollment, replace the mentor or archive the course, then attempt the previously permitted read. Check both visible navigation and direct API requests.

For pagination, verify totals, the second page and filters, not only an empty first page. For writes, verify persistence after refresh and login, duplicate submission behavior and stale versions. For private files, open the actual transferred file and check that an unrelated session cannot download it.

For mood, test disclosure version, the local week boundary, duplicate entries, an empty week and exclusion of private notes from mentor JSON. For self-efficacy, test separate acknowledgment, all three whole-number responses, server mean, the 27/28-day boundary and exclusion of raw answers from mentor responses.

For student UI, include keyboard operation, a 390-pixel screen, long text, upload cancellation, error retry and unsaved navigation. Do not treat browser automation as a substitute for feedback from the intended students.

### 7.4 New computer acceptance

Before depending on the Mac for a demonstration, record the macOS version and architecture, then pass configure and version checks. Verify the running MySQL server and Redis, not merely installed client binaries. Restore the latest intended snapshot and inspect V1 through V9 history.

Test the three roles through the real frontend. Play a video, seek, display a caption and image, upload and download assignment files, submit and review work, and view existing check-in history. Confirm current goals and bookmarks transferred. Check that private notes and raw questionnaire answers remain restricted.

Run F5 from the generated Mac workspace, stop it and run it again without task or port errors. Repeat the intended demo after rebooting the computer. If offline demonstration is needed, download dependencies and local materials beforehand and test offline; external meeting links still require a network.

### 7.5 Ten minute demonstration

Use separate browser sessions for the roles so login channels do not overwrite one another. Prepare clearly named demo accounts and content. Do not invent or backfill private records under real students' identities for the presentation.

| Time | Role and action | What it demonstrates |
|---|---|---|
| 0 to 2 minutes | Administrator creates or opens a course, assigns a mentor and student, then publishes | Allocation, roles and persistent course state |
| 2 to 4 minutes | Mentor opens that course, shows a chapter, uploaded resource and meeting announcement | Teaching workflow and private media metadata |
| 4 to 6 minutes | Student opens the published course, plays media, marks a chapter complete and saves the course | Accessible learning, progress, resume and bookmarks |
| 6 to 8 minutes | Student submits work; mentor grades or returns it; student opens feedback | Real assignment lifecycle and role separation |
| 8 to 9 minutes | Student shows mood history and Self-belief; mentor opens the permitted read-only trend | Distinct schedules, disclosure and privacy boundaries |
| 9 to 10 minutes | Student shows a personal goal; mentor shows Student overview | Private goals and scoped mentor summary |

For new mood or self-efficacy input, use a demo account whose real interval is available. Otherwise show saved history and explain the next available date. Do not disable interval checks for the demonstration. Explain that chart data marked DEMO belongs to prepared fixtures, not observations from actual students.

### 7.6 Accurate answers during review

Completion is reported by the student at chapter level. It is not an automatic assessment of mastery. The self-efficacy function uses an identified short scale and preserves its source, but its implementation has not validated that scale for the target population. Weekly mood replaces the report's original daily-mood frequency.

The application has three public roles and a private maintenance entry. Hiding a button is not the access control: the service and SQL verify identity and current relationships. Mentors can see shared check-ins, while personal notes and raw assessment answers remain private.

Git transfers code. Flyway transfers versioned structure changes. A current SQL snapshot with matching uploads transfers a specific data state. A shared deployed backend is needed for ongoing shared live data across computers.

Linux public deployment and Mac runtime testing are still pending. Cost figures are budgets with named assumptions, not invoices or proof of completed deployment.

### 7.7 Completion checklist for a code change

- Confirm the affected FR and current workflow before adding an interface.
- Keep versions, module boundaries and the shared response and pagination contracts.
- Validate the request and derive trusted identity on the server.
- Enforce role, ownership, current relationship and state at the final data access.
- Use an appropriate transaction, unique constraint, version check or existing request key.
- Add a new migration only when needed; leave applied scripts unchanged.
- Preserve input and provide a clear English result, retry or conflict action.
- Check real media or files and the relevant unauthorized cases.
- Test student accessibility and the permitted role's full browser flow.
- Rebuild, verify copied resources and confirm the generated workspace starts.
- Update this handbook and the FR matrix when actual behavior changes.

## Appendix A Functional requirements

### A1 Assessment rules

The identifiers and requirement names follow the original report's 61 FRs. Completed means the current UI, API and persistence cover the main requirement. Partial means a defined subset exists or an implemented rule differs materially from the report. Not implemented means there is no complete workflow; a generic field, sample title or shared component is not completion.

The current total is 39 completed, 6 partial and 16 not implemented. Authentication and general engineering safeguards are not additional numbered FRs. The report's source is Section 5.4.1, report pages 14 through 22, PDF pages 30 through 38. The original report remains a separate academic source and is not rewritten by this handbook.

| Area | Completed | Partial | Not implemented |
|---|---:|---:|---:|
| Learning FR 01 to 10 | 9 | 1 | 0 |
| Assignments FR 11 to 18 | 6 | 2 | 0 |
| Self-efficacy area FR 19 to 27 | 5 | 1 | 3 |
| Communication FR 28 to 35 | 8 | 0 | 0 |
| AI assistance FR 36 to 42 | 0 | 0 | 7 |
| Entrepreneurship FR 43 to 48 | 2 | 0 | 4 |
| Administration FR 49 to 55 | 6 | 0 | 1 |
| Mentor support FR 56 to 61 | 3 | 2 | 1 |
| Total | 39 | 6 | 16 |

### A2 Complete requirement matrix

| FR | Report requirement | Status | Current implementation and difference |
|---|---|---|---|
| FR-01 | View Course | Completed | Students see enrolled, published courses; no public catalog. |
| FR-02 | Search Course | Completed | Title keyword search is limited to accessible courses. |
| FR-03 | View Course Details | Completed | Description, difficulty and estimated minutes are maintained and displayed. Old defaults need content review. |
| FR-04 | Continue Learning | Completed | Last accessible chapter is saved; resume uses the latest available course. |
| FR-05 | Track Learning Progress | Partial | Visits are saved automatically and chapter completion is explicit. No automatic video completion or mastery measurement. |
| FR-06 | View Learning History | Completed | Paginated currently accessible completed chapters; not an immutable behavior log. |
| FR-07 | Bookmark Course | Completed | Save an enrolled, published course; persists across login and refresh. |
| FR-08 | Manage Bookmark | Completed | Paginated saved list, open and remove; inaccessible courses expose only a removable placeholder. |
| FR-09 | Text-to-Speech | Completed | Read, pause, resume and stop chapter text, subject to available browser voices. |
| FR-10 | Configure Accessibility Settings | Completed | Personal font size, contrast and reduced motion in the browser; no cross-device preference sync. |
| FR-11 | View Assignment | Completed | Scoped assignment list, instructions, due date and status. |
| FR-12 | Submit Assignment | Completed | Formal text/file submission with confirmation and persistence. |
| FR-13 | Upload Answer File | Completed | Private PDF, DOCX, PNG, JPG and TXT attachments; 5 files of 20 MB per attempt. |
| FR-14 | Resubmit Assignment | Partial | Resubmission follows a mentor return, not arbitrary overwriting before the deadline. Late work is allowed and marked. |
| FR-15 | View Assignment Feedback | Completed | Own score, feedback and historical attempts. |
| FR-16 | Create Assignment | Completed | Current mentor creates drafts and publishes within assigned courses. |
| FR-17 | Manage Assignment | Partial | Edit and archive are supported; assignment deletion is not provided. |
| FR-18 | Grade Assignment | Completed | Mentor grades, gives required feedback or returns work; unsent drafts stay private. |
| FR-19 | Daily Check-In | Not implemented | Weekly mood is not a daily attendance workflow. |
| FR-20 | View Check-In Streak | Not implemented | No attendance streak; a trend line is not a streak counter. |
| FR-21 | Record Daily Mood | Partial | Product rule is voluntary weekly mood, not daily mood; saved entries are immutable. |
| FR-22 | View Mood History | Completed | Own history and 4/12/26/52-week trend with text table and explicit gaps. |
| FR-23 | Create Goal | Completed | Student creates a private title and optional notes, stored in MySQL. |
| FR-24 | Manage Goal | Completed | Edit, complete, reopen, filter and delete own goals with version checks. Delete/reopen extend management without adding a new FR. |
| FR-25 | Confidence Assessment | Completed | GSE-3, complete server scoring, 28-day interval, history and trend. Mentors see shared scores, not answers. Target-population validation is separate. |
| FR-26 | Earn Badge | Not implemented | No badge rules or award records. |
| FR-27 | View Learning Analytics | Completed | My learning includes a private statistics page with weighted chapter completion, assignment-state totals and paginated course progress. Reuses current records; no time or mastery estimates. |
| FR-28 | Send Message | Completed | Students and the responsible mentor send scoped text messages in course discussion groups. |
| FR-29 | Receive Message | Completed | The visible room receives new messages through five-second polling and loads earlier history by cursor; no private messaging workflow. |
| FR-30 | Upload File in Chat | Completed | Authenticated discussion attachments support images, documents and voice; previews, downloads and playback enforce membership. |
| FR-31 | Create Group Chat | Completed | The current course mentor creates and maintains discussion groups within a published course. |
| FR-32 | Manage Group Members | Completed | The current mentor adds enrolled students and removes members; revoked membership blocks subsequent reads and writes. |
| FR-33 | Block User | Completed | Confirmed adjustment: students hide another student's messages and attachments for their own view in one course group, and can restore them. The other person is not notified or removed. Mentor and uncertain legacy teaching records remain visible. |
| FR-34 | Report User | Completed | Private message-based reports, own status history and administrator resolution or dismissal; no automatic account ban. |
| FR-35 | Filter Inappropriate Content | Completed | Small explicit phrase list checks discussion text, file names and image descriptions before sharing. Both students and mentors follow it. Rejection preserves unsent input. No inspection of image, document or audio contents. |
| FR-36 | Ask Learning Questions | Not implemented | No AI question service or screen. |
| FR-37 | Generate Learning Summary | Not implemented | No AI summary generation. |
| FR-38 | Explain Difficult Concepts | Not implemented | No AI concept explanation. |
| FR-39 | Generate Practice Questions | Not implemented | No generated practice-question workflow. |
| FR-40 | Provide Assignment Guidance | Not implemented | Mentor feedback is not AI assignment guidance. |
| FR-41 | Generate Encouragement Messages | Not implemented | Fixed UI encouragement is not generated content. |
| FR-42 | Recommend Learning Goals | Not implemented | No personalized goal recommendation. |
| FR-43 | View Entrepreneurship Content | Not implemented | Generic courses and business-themed DEMO titles are not a delivered entrepreneurship curriculum. |
| FR-44 | Use Business Idea Template | Completed | Student-owned guided idea name, offer, audience, resources and next step; partial answers persist and can be edited. |
| FR-45 | View Entrepreneur Stories | Not implemented | No delivered entrepreneur-story collection. |
| FR-46 | Use Cost Calculator | Completed | Server-side MYR batch cost, expected revenue and estimated profit/loss; one versioned saved plan per student. |
| FR-47 | Participate in Market Simulation | Not implemented | No market simulation. |
| FR-48 | Receive Peer Feedback | Not implemented | No peer review of business ideas. |
| FR-49 | Manage Students | Completed | Create, edit, enable/disable and reset permitted student accounts; disable replaces deletion. |
| FR-50 | Manage Mentors | Completed | Create, edit, enable/disable and reset permitted mentor accounts. |
| FR-51 | Manage User Roles | Completed | Assign existing public roles; no custom role-permission editor. |
| FR-52 | Manage Courses | Completed | Course setup, allocation, publication, archive and content management. |
| FR-53 | Manage Assignments | Not implemented | Report assigns this to administrators. Current business rule assigns it only to mentors and protects submission content. |
| FR-54 | Export System Data | Completed | Administrator downloads filtered users, courses and learning-summary CSV files. Private content and credentials are excluded. |
| FR-55 | Configure Accessibility Settings | Completed | Administrator maintains persisted starting size, contrast and reduced motion. Personal Aa choices take priority; no additional main navigation entry. |
| FR-56 | View Student Overview Dashboard | Completed | Scoped student-course completion, assignment counts and latest shared mood, with detail links. |
| FR-57 | Monitor Student Progress | Partial | Mentor sees self-reported chapter completion, not detailed video duration or an activity event stream. |
| FR-58 | Receive At-Risk Student Alerts | Completed | Current mentors see rule-based follow-up reminders for low mood, paused learning and overdue work; reviewed states persist. No diagnostic inference or external notifications. |
| FR-59 | Send Bulk Encouragement Messages | Not implemented | A course announcement is not per-student bulk delivery. |
| FR-60 | Add Mentor Notes | Completed | Author-private mentor observations, with create, paginated history and edit, accessible only while currently responsible for the student. |
| FR-61 | Schedule One-on-One Check-In | Partial | Meeting announcements store a time and link; no individual booking, confirmation, cancellation or conflict checks. |

### A3 Report changes and future work

Keep the distinction between report coverage and intentional product choices. The partial daily-mood requirement is not fixed by relabeling a weekly entry as daily. The unimplemented administrator-assignment requirement is not completed by giving administrators access to student submissions when the current privacy rule excludes them.

Update academic descriptions to match chapter-based self-reporting, current history semantics, controlled resubmission, assignment archive, weekly mood and the GSE-3 implementation. Explain these changes directly. New deletion/reopening controls for personal goals are management details rather than additional numbered FRs.

FR-34 uses message-based private moderation rather than automatic sanctions. FR-54 is limited to three CSV summaries, not unrestricted personal-data export. FR-58 uses the three agreed follow-up rules and an in-app mentor list, rather than diagnostic scoring or external notifications. These are the confirmed product interpretations for this delivery.

Plan the next change from the remaining report requirements and the current code. Do not reimplement completed goals, bookmarks or assessments, or add unrelated infrastructure to make the FR count appear larger.


## Appendix B API reference

All paths below are backend paths without the frontend `/api` proxy prefix. Send `Authorization: Bearer <token>` except for the public login routes and issued media-ticket addresses. Repeated controller prefixes are aliases on one controller, not permission grants: role and service scope still decide which operations can run through a channel.

The tables retain exact method paths and permission strings from the current controllers. `Data` means AjaxResult, `Table` means TableDataInfo, and `File` means a binary ResponseEntity. Path values such as `{id}` must identify an accessible record. Session and service scope means there is no method-specific permission annotation; it does not mean the endpoint is public.

### B1 Login and account administration

Controller [`SysLoginController`](../up-self-admin/src/main/java/com/upself/web/controller/system/SysLoginController.java). Prefixes `/`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| POST | `/empLogin` | Session and service scope | Data |
| POST | `/client/login` | Session and service scope | Data |
| POST | `/empLogin/totp` | Session and service scope | Data |
| GET | `/getInfo`, `/client/getInfo` | Session and service scope | Data |
| GET | `/getRouters`, `/client/getRouters` | Session and service scope | Data |
| PUT | `/system/user/profile/updatePwd`, `/client/profile/password` | Session and service scope | Data |

Controller [`SysUserController`](../up-self-admin/src/main/java/com/upself/web/controller/system/SysUserController.java). Prefixes `/system/user`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/list` | `system:user:list` | Table |
| GET | `/candidates` | `learning:course:assign` | Table |
| GET | `/roles` | `system:user:edit` | Data |
| POST | `/` | `system:user:add` | Data |
| PUT | `/` | `system:user:edit` and `system:user:changeStatus` | Data |
| PUT | `/resetPwd` | `system:user:resetPwd` | Data |
| PUT | `/{id}/resetTotp` | `system:user:resetTotp` | Data |

### B2 Courses content and media

Controller [`LearningController`](../up-self-admin/src/main/java/com/upself/web/controller/learning/LearningController.java). Prefixes `/learning`, `/client/learning`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/courses` | `learning:course:query` | Table |
| GET | `/courses/{id}` | `learning:course:query` | Data |
| POST | `/courses` | `learning:course:add` | Data |
| PUT | `/courses/{id}` | `learning:course:edit` | Data |
| PUT | `/courses/{id}/mentor` | `learning:course:assign` | Data |
| PUT | `/courses/{id}/status` | `learning:course:publish` | Data |
| POST | `/courses/{id}/students` | `learning:course:assign` | Data |
| DELETE | `/courses/{id}/students/{studentId}` | `learning:course:assign` | Data |
| GET | `/courses/{id}/students` | `learning:course:members` | Data |
| GET | `/courses/{id}/chapters` | `learning:chapter:query` | Data |
| GET | `/chapters/{id}` | `learning:chapter:query` | Data |
| POST | `/courses/{id}/chapters` | `learning:chapter:edit` | Data |
| PUT | `/chapters/{id}` | `learning:chapter:edit` | Data |
| DELETE | `/chapters/{id}` | `learning:chapter:edit` | Data |
| GET | `/chapters/{id}/resources` | `learning:resource:query` | Data |
| GET | `/resources/{id}` | `learning:resource:query` | Data |
| POST | `/chapters/{id}/resources` | `learning:resource:upload` | Data |
| PUT | `/resources/{id}` | `learning:resource:upload` | Data |
| DELETE | `/resources/{id}` | `learning:resource:upload` | Data |
| GET | `/courses/{id}/posts` | `learning:post:query` | Data |
| GET | `/posts/{id}` | `learning:post:query` | Data |
| POST | `/courses/{id}/posts` | `learning:post:add` | Data |
| PUT | `/posts/{id}` | `learning:post:edit` | Data |
| PUT | `/posts/{id}/offline` | `learning:post:offline` | Data |
| DELETE | `/posts/{id}` | `learning:post:edit` | Data |

Controller [`LearningMediaController`](../up-self-admin/src/main/java/com/upself/web/controller/learning/LearningMediaController.java). Prefixes `/`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| POST | `/learning/resources/{id}/access`, `/client/learning/resources/{id}/access` | `learning:resource:query` | Data |
| GET | `/learning/resources/{id}/content`, `/client/learning/resources/{id}/content` | `learning:resource:query` | File |
| GET | `/learning/media/{ticket}`, `/client/learning/media/{ticket}` | Session and service scope | File |

### B3 Progress and bookmarks

Controller [`ProgressController`](../up-self-admin/src/main/java/com/upself/web/controller/learning/ProgressController.java). Prefixes `/learning`, `/client/learning`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/courses/{id}/progress` | `learning:progress:own` | Data |
| POST | `/chapters/{id}/visit` | `learning:progress:own` | Data |
| PUT | `/chapters/{id}/completion` | `learning:progress:own` | Data |
| GET | `/resume` | `learning:progress:own` | Data |
| GET | `/history` | `learning:progress:own` | Table |
| GET | `/students/overview` | `learning:students:overview` | Table |

Controller [`BookmarkController`](../up-self-admin/src/main/java/com/upself/web/controller/learning/BookmarkController.java). Prefixes `/learning/bookmarks`, `/client/learning/bookmarks`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/` | `learning:bookmark:own` | Table |
| PUT | `/{courseId}` | `learning:bookmark:own` | Data |
| DELETE | `/{courseId}` | `learning:bookmark:own` | Data |

### B4 Assignments

Controller [`AssignmentController`](../up-self-admin/src/main/java/com/upself/web/controller/assignment/AssignmentController.java). Prefixes `/assignments`, `/client/assignments`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/` | `assignment:read` | Table |
| GET | `/{id}` | `assignment:read` | Data |
| POST | `/` | `assignment:edit` | Data |
| PUT | `/{id}` | `assignment:edit` | Data |
| GET | `/{id}/submission` | `assignment:submit` | Data |
| PUT | `/{id}/submission` | `assignment:submit` | Data |
| POST | `/{id}/submit` | `assignment:submit` | Data |
| POST | `/{id}/files` | `assignment:submit` | Data |
| DELETE | `/{id}/files/{fileId}` | `assignment:submit` | Data |
| GET | `/{id}/submissions` | `assignment:review` | Table |
| GET | `/{id}/submissions/{submissionId}` | `assignment:review` | Data |
| POST | `/{id}/submissions/{submissionId}/review` | `assignment:review` | Data |
| GET | `/files/{fileId}/content` | `assignment:read` | File |

### B5 Weekly mood

Controller [`MoodController`](../up-self-admin/src/main/java/com/upself/web/controller/mood/MoodController.java). Prefixes `/moods`, `/client/moods`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/current` | `mood:own:read` | Data |
| POST | `/acknowledgment` | `mood:own:record` | Data |
| POST | `/entries` | `mood:own:record` | Data |
| GET | `/history` | `mood:own:read` | Table |
| GET | `/trend` | `mood:own:read` | Data |
| GET | `/students` | `mood:students:read` | Table |
| GET | `/students/{studentId}` | `mood:students:read` | Data |
| GET | `/students/{studentId}/history` | `mood:students:read` | Table |
| GET | `/students/{studentId}/trend` | `mood:students:read` | Data |

### B6 General self efficacy

Controller [`SelfEfficacyController`](../up-self-admin/src/main/java/com/upself/web/controller/selfefficacy/SelfEfficacyController.java). Prefixes `/self-efficacy`, `/client/self-efficacy`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/current` | `efficacy:own:read` | Data |
| POST | `/acknowledgment` | `efficacy:own:record` | Data |
| POST | `/entries` | `efficacy:own:record` | Data |
| GET | `/history` | `efficacy:own:read` | Table |
| GET | `/trend` | `efficacy:own:read` | Data |
| GET | `/students/{studentId}` | `efficacy:students:read` | Data |
| GET | `/students/{studentId}/history` | `efficacy:students:read` | Table |
| GET | `/students/{studentId}/trend` | `efficacy:students:read` | Data |

### B7 Personal goals

Controller [`GoalController`](../up-self-admin/src/main/java/com/upself/web/controller/goal/GoalController.java). Prefixes `/client/goals`.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/` | `goals:own` | Table |
| GET | `/{id}` | `goals:own` | Data |
| POST | `/` | `goals:own` | Data |
| PUT | `/{id}` | `goals:own` | Data |
| PUT | `/{id}/completion` | `goals:own` | Data |
| DELETE | `/{id}` | `goals:own` | Data |

Logout is implemented by Spring Security rather than a controller method: `POST /client/logout` and `POST /logout` invalidate the current channel session. Use the matching getInfo and getRouters endpoints after login. Login returns a top-level token; getInfo returns user, roles, permissions and totpEnabled. The retained TOTP route is disabled in the current configuration.

### B8 Request fields and examples

List endpoints use `current` and `pageSize` where they return TableDataInfo. Course and student lists add the documented keyword filter. Account candidates require `role=student` or `role=mentor`. Assignment lists accept keyword, courseId and pending. Mentor overview accepts keyword and an optional courseId. Goal lists accept ACTIVE or COMPLETED as an optional status filter.

Course creation or update uses title, description, difficulty, estimatedMinutes and languageCode. Mentor assignment uses mentorId; enrollment uses studentId; course publication uses status.

```json
{
  "title": "Introduction to planning",
  "description": "Work through one small planning task.",
  "difficulty": "BEGINNER",
  "estimatedMinutes": 30,
  "languageCode": "en"
}
```

Chapter input uses title, body, sortOrder, status and languageCode. Resource upload is multipart with file, kind, title, status, languageCode and optional altText, captionResourceId and transcript. Resource update uses its metadata body without a replacement binary file. Post input uses title, body, type, meetingUrl, meetingStartsAt, status and languageCode.

```json
{
  "title": "Weekly discussion",
  "body": "Bring one question from the course.",
  "type": "MEETING",
  "meetingUrl": "https://example.org/meeting",
  "meetingStartsAt": "2026-11-01T10:00:00+08:00",
  "status": "PUBLISHED",
  "languageCode": "en"
}
```

Assignment input uses courseId, title, instructions, status, dueAt, maxScore, version and languageCode. Draft input uses version and answerText. Formal submit adds assignmentVersion and requestKey. File upload uses multipart file and version; draft file removal sends version as a query parameter. Review uses version, decision GRADED or RETURNED, feedback and score when grading.

```json
{
  "version": 2,
  "assignmentVersion": 1,
  "answerText": "My completed work.",
  "requestKey": "a3bb189e-8bf9-4888-9912-ace4e6543002"
}
```

Use versions returned by the server. The example is not a fixed request key to reuse across different submissions. Formal submission returns the result described by the assignment feature; it is not merely a file-upload acknowledgment.

Mood acknowledgment uses noticeVersion and accepted=true. Mood entry uses weekStart, moodLevel and optional privateNote. Use the week and disclosure version returned by current. Mood levels are integers 1 through 5. Trend APIs accept weeks from the supported 4-to-52 range; the UI offers its four named presets.

Self-efficacy acknowledgment uses its own noticeVersion and accepted=true. Entry uses assessmentDate, instrumentVersion and a three-element answers array. Obtain current dates and versions from current rather than hardcoding them. No input field may choose another student's identity or final score.

```json
{
  "assessmentDate": "2026-10-25",
  "instrumentVersion": "GSE-3-EN-2021-v1",
  "answers": [1, 2, 2]
}
```

The sample assessmentDate is illustrative. A request must match the actual Malaysia date and current eligibility. Goal create uses title and details. Goal update adds version. Completion uses completed and version. Goal delete requires the version query parameter. Chapter completion uses a completed boolean.

### B9 Response privacy and errors

Use the common contracts in Section 2. A paginated response has top-level rows and total. A detail response usually has data. Preserve privacy projections even when returning a list: mentor mood rows exclude privateNote, mentor assessment rows exclude raw answers, and an unavailable bookmark contains course:null.

An unauthenticated request, wrong channel, wrong role and inaccessible relationship can fail at different layers. Do not infer authorization solely from HTTP status. Handle the JSON business code and test the actual role boundary with direct requests as well as the UI.

### B10 Mentor notes and business practice

Mentor routes use /mentorship/students/{studentId}/notes. GET /student returns scoped student metadata; GET on the base lists the author's notes with current and pageSize. POST creates {content}; PUT /{id} edits {content,version}. All reads and writes require the responsible mentor and enforce author ownership. The /client alias exists for consistent explicit role-denial checks; students have no permission to use it.

Student routes use /client/business. GET /ideas lists private ideas; POST creates {title,offer,audience,resources,nextStep}; GET /ideas/{id} reads one; PUT /ideas/{id} edits those fields plus version. GET /cost-plan returns the current plan or saved:false with version:0. POST /cost-plan/calculate accepts materialsCost, otherCosts, quantity and pricePerItem. PUT /cost-plan saves the same inputs plus version. Money values may be sent as decimal strings. The /business alias applies the same service role checks and never grants management accounts access.

Lists use TableDataInfo; details and calculations use AjaxResult. Run `python scripts/verify-business-notes.py --browser` with local services running. It creates disposable fixtures, checks role and owner denial, relationship revocation, pagination, conflicting edits, decimal arithmetic and Chrome workflows, then removes only its own rows. Logs and screenshots are in .local/mvp-verification.

### B11 Reports follow-ups and exports

| Method | Path | Access and response |
|---|---|---|
| POST | `/reports` or `/client/reports` | Student/mentor; messageId, reason and optional details; AjaxResult with report ID |
| GET | `/reports/mine` or `/client/reports/mine` | Own status history; TableDataInfo |
| GET | `/moderation/reports` | Public administrator; status filter and TableDataInfo |
| GET | `/moderation/reports/{id}` | Public administrator; saved evidence in AjaxResult |
| PUT | `/moderation/reports/{id}` | Public administrator; RESOLVED or DISMISSED, resolution and version |
| GET | `/follow-ups` | Current mentor; status and optional studentId filters; TableDataInfo |
| GET | `/follow-ups/summary` | Current mentor; pending total and per-student counts in AjaxResult |
| PUT | `/follow-ups/{id}/review` | Current mentor; version required |
| GET | `/exports/users` | Public administrator; keyword, role and status filters; CSV download |
| GET | `/exports/courses` | Public administrator; keyword and status filters; CSV download |
| GET | `/exports/learning` | Public administrator; keyword and courseId filters; CSV download |

List pagination follows `current` and `pageSize` through BaseController and TableDataInfo. Management APIs also register `/client` aliases so wrong-role calls are rejected consistently; an alias grants no access. CSV bypasses the JSON success envelope only for a successful download. Error responses retain the existing application envelope.

### B12 Platform defaults and student statistics

| Method | Path | Access and response |
|---|---|---|
| GET | `/accessibility/defaults` | Public, non-personal size/contrast/motion/version in AjaxResult; no-store |
| PUT | `/accessibility/defaults` | Public platform-administrator role and settings:accessibility:edit; validated size, contrast, motion and version |
| GET | `/client/learning/statistics` | Student with learning:statistics:own; authenticated student's totals in AjaxResult |
| GET | `/client/learning/statistics/courses` | Same scope; current/pageSize and TableDataInfo |

The settings controller also has a `/client/accessibility/defaults` alias for role rejection through the student channel. Only the exact GET `/accessibility/defaults` is anonymous; PUT is not whitelisted. Progress-controller `/learning` aliases retain their existing role checks. None of the statistics routes can select another student by ID.

### B13 Course discussion safety

Prefixes: `/chat` and `/client/chat`. `chat:read` and the service's current role/course/group checks apply to all reads. Management and student aliases do not bypass the service checks.

| Method | Path after prefix | Permission | Response |
|---|---|---|---|
| GET | `/groups/{id}/sync?afterId=...` | `chat:read`; current student member or current mentor | Data |
| GET | `/groups/{id}/messages?afterId=...` or `?beforeId=...` | `chat:read`; one cursor only | Data |
| GET | `/groups/{id}/members` | `chat:read`; includes only the viewer's visibility preferences | Data |
| PUT | `/groups/{id}/hidden-peers/{studentId}` | `chat:hide`; student viewer and another current student member | Data |
| DELETE | `/groups/{id}/hidden-peers/{studentId}` | `chat:hide`; student viewer clearing their own setting | Data |
| POST | `/groups/{id}/messages` | `chat:send`; current published/active group; per-user limit | Data |
| POST | `/groups/{id}/files` | `chat:send`; multipart file and optional altText; per-user limit | Data |
| GET | `/files/{id}/content` | `chat:read`; current scope and visibility restriction | File |

Sync returns `data: {group, messages: {rows, hasMore, nextCursor}, hiddenStudentIds}`. Without afterId it returns the latest visible window; with afterId it returns newer visible rows in ascending order, at most 30. Follow nextCursor even when rows is empty; when hasMore is true request the next batch promptly. Earlier-history navigation uses beforeId on the messages endpoint. Message objects include authenticated senderRole and hideAllowed. Send input remains body, fileIds and clientKey; sender identity and role supplied by a client are never trusted. Code 422 requests a wording change; code 429 requests a pause. Preserve the unsent composer on either error.

## Appendix C Source and maintenance index

### C1 Backend source map

The following links point to current source files, not a copied implementation inside this handbook.

- [pom.xml](../pom.xml).
- [up-self-admin/pom.xml](../up-self-admin/pom.xml).
- [up-self-admin/src/main/resources/application.yml](../up-self-admin/src/main/resources/application.yml).
- [up-self-common/src/main/java/com/upself/common/core/controller/BaseController.java](../up-self-common/src/main/java/com/upself/common/core/controller/BaseController.java).
- [up-self-common/src/main/java/com/upself/common/core/page/TableSupport.java](../up-self-common/src/main/java/com/upself/common/core/page/TableSupport.java).
- [up-self-framework/src/main/java/com/upself/framework/config/SecurityConfig.java](../up-self-framework/src/main/java/com/upself/framework/config/SecurityConfig.java).
- [up-self-framework/src/main/java/com/upself/framework/config/DruidConfig.java](../up-self-framework/src/main/java/com/upself/framework/config/DruidConfig.java).
- [up-self-framework/src/main/java/com/upself/framework/config/MyBatisConfig.java](../up-self-framework/src/main/java/com/upself/framework/config/MyBatisConfig.java).
- [up-self-framework/src/main/java/com/upself/framework/web/exception/GlobalExceptionHandler.java](../up-self-framework/src/main/java/com/upself/framework/web/exception/GlobalExceptionHandler.java).
- [up-self-framework/src/main/java/com/upself/framework/web/service/SysLoginService.java](../up-self-framework/src/main/java/com/upself/framework/web/service/SysLoginService.java).
- [up-self-framework/src/main/java/com/upself/framework/web/service/TokenService.java](../up-self-framework/src/main/java/com/upself/framework/web/service/TokenService.java).
- [up-self-framework/src/main/java/com/upself/framework/web/service/LearningMediaService.java](../up-self-framework/src/main/java/com/upself/framework/web/service/LearningMediaService.java).
- [up-self-biz/src/main/resources/mapper/learning/LearningMapper.xml](../up-self-biz/src/main/resources/mapper/learning/LearningMapper.xml).
- [up-self-biz/src/main/resources/mapper/learning/ProgressMapper.xml](../up-self-biz/src/main/resources/mapper/learning/ProgressMapper.xml).
- [up-self-biz/src/main/resources/mapper/learning/BookmarkMapper.xml](../up-self-biz/src/main/resources/mapper/learning/BookmarkMapper.xml).
- [up-self-biz/src/main/resources/mapper/assignment/AssignmentMapper.xml](../up-self-biz/src/main/resources/mapper/assignment/AssignmentMapper.xml).
- [up-self-biz/src/main/resources/mapper/mood/MoodMapper.xml](../up-self-biz/src/main/resources/mapper/mood/MoodMapper.xml).
- [up-self-biz/src/main/resources/mapper/goal/GoalMapper.xml](../up-self-biz/src/main/resources/mapper/goal/GoalMapper.xml).
- [up-self-biz/src/main/resources/mapper/selfefficacy/SelfEfficacyMapper.xml](../up-self-biz/src/main/resources/mapper/selfefficacy/SelfEfficacyMapper.xml).
- [up-self-biz/src/main/java/com/upself/biz/selfefficacy/service/SelfEfficacyScale.java](../up-self-biz/src/main/java/com/upself/biz/selfefficacy/service/SelfEfficacyScale.java).


New backend sources:

- [ReportServiceImpl.java](../up-self-biz/src/main/java/com/upself/biz/report/service/impl/ReportServiceImpl.java).
- [FollowUpServiceImpl.java](../up-self-biz/src/main/java/com/upself/biz/followup/service/impl/FollowUpServiceImpl.java).
- [ExportServiceImpl.java](../up-self-biz/src/main/java/com/upself/biz/reporting/service/impl/ExportServiceImpl.java).
- [V14 migration](../up-self-admin/src/main/resources/db/draft/V14__reports_follow_ups_and_exports.sql).

Additional V15 sources:

- [AccessibilityController.java](../up-self-admin/src/main/java/com/upself/web/controller/system/AccessibilityController.java).
- [AccessibilityServiceImpl.java](../up-self-system/src/main/java/com/upself/system/service/impl/AccessibilityServiceImpl.java).
- [AccessibilityMapper.xml](../up-self-system/src/main/resources/mapper/system/AccessibilityMapper.xml).
- [ProgressMapper.xml](../up-self-biz/src/main/resources/mapper/learning/ProgressMapper.xml).
- [V15 migration](../up-self-admin/src/main/resources/db/draft/V15__accessibility_defaults_and_learning_statistics.sql).

Additional V16 sources:

- [ChatController.java](../up-self-admin/src/main/java/com/upself/web/controller/chat/ChatController.java).
- [ChatServiceImpl.java](../up-self-biz/src/main/java/com/upself/biz/chat/service/impl/ChatServiceImpl.java).
- [ChatContentFilter.java](../up-self-biz/src/main/java/com/upself/biz/chat/service/ChatContentFilter.java).
- [ChatMapper.xml](../up-self-biz/src/main/resources/mapper/chat/ChatMapper.xml).
- [V16 migration](../up-self-admin/src/main/resources/db/draft/V16__chat_visibility_and_content_filter.sql).
- [useDiscussion.ts](../frontend/src/features/chat/composables/useDiscussion.ts).
- [Chat safety verification](../scripts/verify-chat-safety.py).

### C2 Frontend and operational source map

- [frontend/package.json](../frontend/package.json).
- [frontend/package-lock.json](../frontend/package-lock.json).
- [frontend/vite.config.ts](../frontend/vite.config.ts).
- [frontend/src/app/router.ts](../frontend/src/app/router.ts).
- [frontend/src/app/layouts/AppLayout.vue](../frontend/src/app/layouts/AppLayout.vue).
- [frontend/src/shared/http/client.ts](../frontend/src/shared/http/client.ts).
- [frontend/src/shared/http/contracts.ts](../frontend/src/shared/http/contracts.ts).
- [frontend/src/shared/ui/NavigationLinks.vue](../frontend/src/shared/ui/NavigationLinks.vue).
- [frontend/src/shared/styles/base.css](../frontend/src/shared/styles/base.css).
- [frontend/src/shared/i18n/index.ts](../frontend/src/shared/i18n/index.ts).
- [scripts/configure-local.ps1](../scripts/configure-local.ps1).
- [scripts/configure-services.ps1](../scripts/configure-services.ps1).
- [scripts/database.ps1](../scripts/database.ps1).
- [scripts/prepare-debug.ps1](../scripts/prepare-debug.ps1).
- [scripts/macos/up-self.sh](../scripts/macos/up-self.sh).
- [scripts/macos/tools.mjs](../scripts/macos/tools.mjs).
- [scripts/macos/database.mjs](../scripts/macos/database.mjs).
- [up-self-macos.code-workspace](../up-self-macos.code-workspace).
- [scripts/package-linux.ps1](../scripts/package-linux.ps1).
- [deploy/linux/preflight.sh](../deploy/linux/preflight.sh).
- [deploy/linux/run.sh](../deploy/linux/run.sh).
- [deploy/linux/backup.sh](../deploy/linux/backup.sh).
- [deploy/linux/application-prod.yml.example](../deploy/linux/application-prod.yml.example).
- [deploy/linux/up-self.service.example](../deploy/linux/up-self.service.example).
- [deploy/linux/nginx-https.conf.example](../deploy/linux/nginx-https.conf.example).
- [environment/versions.json](../environment/versions.json).


### C3 Documentation maintenance

Change the handbook when an API, permission, workflow, runtime requirement or verified platform status changes. Update Appendix A from actual implementation, keeping all 61 report identifiers exactly once in its main matrix. Record what was tested and on which platform; do not turn a preparation check into a runtime claim.

Keep detailed resolved dependency evidence in the two retained JSON files and the actual lockfile. Do not paste the full transitive dependency tree into ordinary setup instructions. Keep screenshots only when they explain current behavior and have a clear date and caption. Historical menu screenshots are not authoritative navigation specifications.

The docs index identifies short compatibility files retained for existing scripts and metadata. They are generated or redirect to this handbook, not additional sources to edit. Business code, startup configuration and reference-project files are outside a documentation cleanup.

Regenerate the Word document from this Markdown source after an update, refresh the contents and page fields, and check the rendered pages. Keep the title, heading hierarchy, repeated table headers and readable command blocks. Check local source links after moving or removing a file.
