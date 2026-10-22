# Actual local snapshot

This directory intentionally contains plaintext local credentials, the actual UP-SELF MySQL database including flyway_schema_history, and the matching uploaded files, as explicitly requested by the owner. It contains no reference-project files or database.

The current actual application credentials and bootstrap passwords are in config/application-local.yml at the repository root. The snapshot local-credentials.json includes the existing MySQL root, application, migration, and Redis credentials. Restoring a snapshot does not reset an existing MySQL root password.

Do not import the SQL into the reference database or a database with other work. Restore only into an empty, independent up_self_dev database. Restore the matching uploads to .local/uploads. New-machine installation and validated restore instructions will be delivered in the portability follow-up commit.

No dependencies, service binaries, caches, reference configuration or GitHub credentials are included. This snapshot contains the actual currently stored demo course; it is not a set of newly fabricated records.
