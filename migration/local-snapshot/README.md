# Actual local snapshot

This directory intentionally contains plaintext local credentials, the actual UP-SELF MySQL database including flyway_schema_history, and the matching uploaded files, as explicitly requested by the owner. It contains no reference-project files or database.

The current actual application credentials and bootstrap passwords are in config/application-local.yml at the repository root. The snapshot local-credentials.json includes the existing MySQL root, application, migration, and Redis credentials. Restoring a snapshot does not reset an existing MySQL root password.

Do not import the SQL into the reference database or a database with other work. Restore only into an empty, independent up_self_dev database with scripts/database.ps1 -Action Restore. It verifies all manifest hashes before restoring the matching uploads to .local/uploads, including assignments/ when real submission files exist. See [the installation and startup guide](../../docs/跨电脑环境安装与启动教程.md).

This snapshot includes Flyway V1-V5, learning, assignments and weekly mood data. Exact creation time, row counts and file hashes are in manifest.json. No new demonstration records were fabricated. Redis login sessions are not exported. Existing databases should receive missing migrations at backend startup, not be overwritten with this snapshot.

To refresh this snapshot later, pause application writes and run scripts/database.ps1 -Action Export -MySqlHome <installed MySQL directory>, then validate Restore in a separate empty database before committing. Export reads only up_self_dev, copies referenced files and does not change passwords.

No dependencies, service binaries, caches, reference configuration or GitHub credentials are included. This snapshot contains the actual currently stored demo course; it is not a set of newly fabricated records.
