# Local migration snapshot

This directory contains a dated export of the independent UP-SELF development database and its matching private uploads. Inspect manifest.json for the actual creation time, migration versions, file counts and SHA-256 values.

The latest export was refreshed on 28 October 2026 after the demo-account reset, with migrations V1 through V12 applied. It includes six students, two mentors, one administrator, the unchanged internal maintenance account, and the current synthetic demonstration records. Current public demo logins are listed in [config/demo-accounts.yml](../../config/demo-accounts.yml). Use manifest.json as the authority for the snapshot's exact contents and export time.

This is an export, not a live database mirror. Later changes appear here only after another export. Applying Flyway migrations creates missing schema and permissions; it does not transfer records created on another computer.

For current data migration, pause application writes on the source computer, export the latest database with matching referenced uploads, then transfer the whole directory with the corresponding code and application configuration. Do not restore over an existing populated database or merge upload directories.

The [Developer Handbook](../../docs/UP-SELF-Developer-Handbook.md#5-local-setup-and-data-migration) contains the maintained export and restore procedures. Mac restore deliberately requires a snapshot matching the current source migrations. Redis login sessions do not need to be transferred.

Snapshot credentials and application configuration are currently tracked by the repository owner. Keep actual values in their existing files rather than duplicating them in documentation. Do not substitute reference-project data or credentials.
