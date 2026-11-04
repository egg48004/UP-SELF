# Local migration snapshot

This directory is a dated logical export of the independent UP-SELF development database plus the upload files collected by the existing snapshot tool. Inspect manifest.json for the actual export date, migration versions, file list and SHA-256 hashes. The inspected archive is dated 30 October 2026 and records V1 through V17 with 12 collected uploads.

The current exporter dumps every database table, but its upload collector only queries learning_resource and assignment_file. It does not collect chat files or diary recording keys, and assignment audio uses the shared recordings directory rather than the assumed assignment directory. Therefore this archive must not be treated as a complete chat/recording migration package.

For another Windows computer, follow [Windows Migration and Development Handoff](../../docs/Windows-Migration-and-Agent-Handoff.md). That workflow exports the latest live database separately and copies the entire active private upload tree while application writes are stopped. Do not edit this archive's manifest to make an incomplete file set appear complete.

Git is not a live database mirror. Later accounts, submissions and messages move only through another export or a shared deployed backend. Flyway applies schema changes; it does not transfer later records. Redis sessions do not need to move.

The current demo inventory is [config/demo-accounts.yml](../../config/demo-accounts.yml). Actual configuration and migration credentials stay in their established files. Keep the working source until the new computer passes database, private-file, browser and VS Code checks. UP-SELF itself is the development baseline; the old reference project is not required.