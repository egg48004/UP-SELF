# Archived local migration snapshot

This directory contains a dated export of the independent UP-SELF development database and its matching private uploads. Inspect manifest.json for the actual creation time, migration versions, file counts and SHA-256 values.

The archived snapshot currently represents the V5 data state from 22 October 2026. It is not a live mirror of the current V9 application database. Later records are absent until a fresh export is explicitly generated. Applying V6 through V9 adds their schema and permission changes, not missing historical business data.

For current data migration, pause application writes on the source computer, export the latest database with matching referenced uploads, then transfer the whole directory with the corresponding code and application configuration. Do not restore over an existing populated database or merge upload directories.

The [Developer Handbook](../../docs/UP-SELF-Developer-Handbook.md#5-local-setup-and-data-migration) contains the maintained export and restore procedures. Mac restore deliberately requires a snapshot matching the current source migrations. Redis login sessions do not need to be transferred.

Snapshot credentials and application configuration are currently tracked by the repository owner. Keep actual values in their existing files rather than duplicating them in documentation. Do not substitute reference-project data or credentials.
