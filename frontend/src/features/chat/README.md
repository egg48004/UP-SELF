# Course discussions

Course discussions support FR-28 to FR-32: text messages, receiving messages, private attachments, mentor-created groups and member management. Voice messages extend the chat attachment workflow. Blocking, reports, automated moderation, calls and transcription are not implemented.

## Boundaries

- The entry point is Discussion inside a course. The five student main navigation entries are unchanged.
- The current course mentor manages groups. Students need an active account, current course enrollment and active group membership.
- Students can access discussions in published courses only. Archiving or unpublishing a course removes their access; data is retained. Closed groups remain readable to eligible members but reject sends and uploads.
- New members can read previous messages. Removing a member revokes future history and file requests. Re-enrolling an existing active group member restores access. A replacement course mentor inherits group management; the previous mentor loses access.
- Administrators and maintenance accounts have no chat-content bypass.

## Files and recordings

Unsent attachments belong to their uploader and stay private until Send. Pending uploads survive refresh, with at most five per uploader per group; the owner can remove them. Sent files cannot be removed through the draft endpoint.

Supported documents are PDF, DOCX and UTF-8 TXT, and images are PNG/JPG, up to 20 MB each. Images require a short description. Audio uploads are limited to 5 MB. The browser recorder stops after 60 seconds; the server enforces audio format and byte size, not the duration of imported compressed audio. There is no transcription or transcoding service.

PrivateFileStorage shares the existing format validation between assignments and chat. Document/image folders remain separate; recordings use the existing private recordings folder. Reads go through authenticated backend endpoints, never public static paths. ChatAttachment and AudioPlayer fetch blobs on demand and release object URLs on unmount.

## Message updates

useDiscussion loads 30 recent messages and pages older history by message ID. While visible, the open room polls every five seconds for newer IDs only. Hidden tabs pause, failures back off, and unmount aborts the current request. A revoked session or membership clears cached room content on the next failed request.

Writes lock the course, then the group, under READ_COMMITTED. Enrollment and mentor changes use the same course lock. This serializes message inserts so committed cursor reads do not skip an earlier uncommitted message. A unique client send key makes retries idempotent; a reused key with different content is rejected.

## Verification

Run scripts/verify-chat.py --browser from the project root with the intended local services running. UPSELF_TEST_API_URL selects localhost 8080 or 8081; UPSELF_TEST_UI_URL selects the corresponding Vite server. Tests create disposable course/account fixtures and remove only their own rows and upload keys. Logs and screenshots go to .local/chat-verification. Chrome audio tests use MediaRecorder with a simulated microphone and check actual decoding/playback.

V12__course_group_chat.sql adds the four chat tables and role permissions. Existing signed-in users should sign out and back in after the migration to refresh permissions. No dependency, reference-project or startup-configuration changes are required.
