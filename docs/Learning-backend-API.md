# Learning backend API

Implemented in the existing six-module Java architecture, with unchanged dependency versions. Vue learning screens are implemented in frontend/. Assignments, mood tracking and chat are outside this delivery.

## Permissions and workflow

| Actor | Allowed actions |
|---|---|
| Platform administrator | Create/edit courses, assign one active mentor, enroll/remove students, publish/archive courses; manage content and take posts offline |
| Mentor | Read assigned courses and membership; maintain chapters/resources; create and maintain their own posts in assigned courses |
| Student | Read joined, published courses and their published chapters/resources/posts |
| Maintenance | Existing superuser permissions; reserved for platform maintenance |

Authorization is enforced by permission annotations and database ownership/enrollment checks. The server takes author identity from the session. Reassigning a course removes the former mentor's access; a replacement mentor cannot edit the former mentor's posts. Archived courses reject content changes. Account/password login, Redis sessions, channel separation and revocation remain; two-step verification stays disabled.

1. Administrator obtains user IDs from existing account management, creates a course, assigns a mentor and enrolls students.
2. Mentor creates chapters and uploads content, initially as DRAFT if desired.
3. Mentor publishes chapters/resources/posts. Administrator publishes the course after assigning an active mentor.
4. Student reads content. Removing enrollment, unpublishing a course or logging out blocks subsequent media requests.

## Routes

Management prefix: `/learning`. Student prefix: `/client/learning`. Send `Authorization: Bearer <token>` except for issued media-ticket URLs. Mutating endpoints remain subject to role permissions even if called with a student prefix.

| Method | Path after prefix | Purpose |
|---|---|---|
| GET | /courses?current=1&pageSize=10&keyword=example | Scoped course list, existing PageHelper result |
| GET, POST | /courses | POST creates course (GET lists) |
| GET, PUT | /courses/{id} | Course detail/update |
| PUT | /courses/{id}/mentor | Assign `{ "mentorId": 123 }` |
| PUT | /courses/{id}/status | `{ "status": "PUBLISHED" }` |
| GET | /courses/{id}/students | Membership list |
| POST | /courses/{id}/students | Enroll `{ "studentId": 124 }` |
| DELETE | /courses/{id}/students/{studentId} | Remove enrollment |
| GET, POST | /courses/{id}/chapters | List/create chapters |
| GET, PUT, DELETE | /chapters/{id} | Read/update/soft-delete chapter |
| GET, POST | /chapters/{id}/resources | List/upload resources |
| GET, PUT, DELETE | /resources/{id} | Metadata/update/soft-delete |
| POST | /resources/{id}/access | Issue media-ticket URL |
| GET | /resources/{id}/content | Media with Bearer authentication |
| GET | /media/{ticket} | Media with opaque ticket and live authorization |
| GET, POST | /courses/{id}/posts | List/create posts |
| GET, PUT, DELETE | /posts/{id} | Read/update/soft-delete post |
| PUT | /posts/{id}/offline | Take post offline |

Course page size is capped at 100. Chapter, resource and post collections are unpaged. Responses reuse AjaxResult; inspect its `code` and `msg`, not only HTTP status. IDs in requests must refer to accessible records.

## Request examples

Course POST/PUT:
```json
{"title":"Introduction","description":"Course description","languageCode":"en"}
```

Chapter POST/PUT:
```json
{"title":"Week 1","body":"Plain text introduction","sortOrder":0,"status":"PUBLISHED","languageCode":"en"}
```

Post POST/PUT:
```json
{"title":"Weekly meeting","body":"Please join our session.","type":"MEETING","meetingUrl":"https://example.org/meeting","meetingStartsAt":"2026-11-01T10:00:00+08:00","status":"PUBLISHED","languageCode":"en"}
```

Post types are ANNOUNCEMENT and MEETING; MEETING requires an HTTPS URL. Meeting time, when supplied, uses ISO 8601 with an offset. This stores a meeting link, not a video-conference service. Text fields are plain text; future UI must escape them.

Course states: DRAFT/PUBLISHED/ARCHIVED. Chapter/resource/post states: DRAFT/PUBLISHED. Students cannot read draft content, including published resources inside a draft chapter.

## Uploads and accessible media

Upload is multipart/form-data: `file`, `kind`, `title`, `status`, `languageCode`, and optional `altText`, `captionResourceId`, `transcript`.

| kind | Accepted content | Limit |
|---|---|---|
| IMAGE | JPEG, PNG, WebP | 10 MB; meaningful altText required |
| VIDEO | MP4 | 100 MB |
| CAPTION | WebVTT (.vtt) | 1 MB |

Extension and content signatures are checked. SVG is not accepted. MP4 is not transcoded; upload browser-compatible video (typically H.264/AAC). Header/range tests do not replace checking playback of actual teaching videos in the eventual Vue interface.

Upload a caption into the same chapter first, then reference its ID from the video's `captionResourceId`. A published video requires its linked caption to be published. Referenced captions cannot be deleted or unpublished while still used. `transcript` reserves a textual transcript; subtitles/transcripts are provided by the mentor, not generated automatically.

Metadata never returns a physical storage key. Files are private in `learning.storage-path` (default `.local/uploads`) with random names. They are not exposed through a public static directory. File rollback cleanup accompanies failed database transactions. Content deletion is soft deletion and retains disk files; archive/retention cleanup is not implemented. Preserve this directory alongside database backups on deployment.

For future native video/img/track elements, call POST `/resources/{id}/access`. Its `data` contains a relative `url` and `expiresIn` (1800 seconds). Set the element source to that URL on the backend origin. Tickets contain no JWT and are Redis-backed; every request checks the live session, account, permissions, course, enrollment and publication state. URLs are still temporary credentials: do not log/share them. Refresh after expiry. Byte ranges return 206, supporting seeking. Use a separate authorized ticket for WebVTT `<track>` sources. Direct Bearer content requests are also supported.

## English and future languages

Current API messages, validation and the existing account verification page use English. MessageSource catalogs and a fixed English locale are in place; language switching is not implemented. Course/chapter/resource/post metadata includes `languageCode`, default en. These fields identify content language; they do not automatically translate content or model multiple translations of one record. Future Vue UI should use translation keys, render image altText and expose captions/transcripts.

## Source and verification

- Business: `up-self-biz/.../com/upself/biz/learning` and `mapper/learning/LearningMapper.xml`.
- Controllers: `up-self-admin/.../web/controller/learning`.
- Media sessions: `up-self-framework/.../web/service/LearningMediaService.java`.
- Language configuration: `LanguageConfig.java`, `i18n/messages.properties`, `ValidationMessages.properties`.
- Schema/permissions: `db/draft/V3__learning_and_english_permissions.sql`. V1/V2 are unchanged; V3 has executed locally and must not be edited.
- `scripts/verify-learning.py`: isolated integration fixtures, cleanup after execution; stop application before running.
- `scripts/verify-local.py`: existing login/account regression; stop application before running.

Verified: 18 unit tests, 85 learning integration checks, 40 authentication/account regression checks. Test fixtures include byte-range media samples; no actual course material or unrelated reference business data was copied. Vue learning pages are now available at http://127.0.0.1:5173 . The backend root page remains an account/permission verification tool.

## VS Code launch verification

F5 started UpSelfApplication successfully on port 8080; VS Code reported No Problems and Java Ready. Student and mentor login/course-list/logout passed against this process, and the English verification page returned HTTP 200. The unfinished local YAML edit was repaired. Subsequently, at the user request, admin01 was reset through the maintenance password-reset API and the new password was saved to bootstrap.passwords.admin01 in config/application-local.yml. Actual admin01 login, course creation/editing, mentor assignment, enrollment/removal, publication and archiving were verified; temporary course data was removed. MySQL account passwords were not changed. A root/root TCP login to 127.0.0.1:3306 was rejected; this was reported without changing root credentials.

## P0 pagination alignment

Controllers now extend the reference-style BaseController, use startPage()/getDataTable() and return TableDataInfo for paginated lists. The public page parameter is current; pageNum remains a temporary compatibility alias for existing clients. Both provided: current wins. Max page size remains 100. Non-paginated chapter/resource/post collections retain data arrays.

## FR-07/08 course bookmarks (V8)

Student permission: `learning:bookmark:own`. GET `/client/learning/bookmarks?current=1&pageSize=10` returns `rows,total,code,msg`. Each row contains `courseId,savedAt,available,course`. PUT and DELETE `/client/learning/bookmarks/{courseId}` save/remove the authenticated student's bookmark and return AjaxResult; no body or studentId is needed. Repeated saves/removals are idempotent. Existing course list/detail responses now include the current student's `bookmarked` boolean.

Only enrolled, published, non-deleted courses may be saved. Inaccessible saved entries remain removable with `available:false,course:null`; their previous title/description/mentor are not exposed. Bookmarks do not grant enrollment or course access. Mentor, administrator and maintenance cannot use student bookmarks. No request parameter can select another owner.

Source: `BookmarkController`, `BookmarkServiceImpl`, `BookmarkMapper.xml` and `V8__course_bookmarks.sql`. Reproduce the real API/Chrome workflow with `python scripts/verify-bookmarks.py --browser` while local frontend/backend are running. Full Chinese delivery notes: [FR-07/08](课程收藏FR07-FR08交付验收.md).
