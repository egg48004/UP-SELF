-- Owner-approved policy change: all earlier diary test data is now mentor-visible.
UPDATE mood_entry SET note_shared=1 WHERE note_shared=0;
ALTER TABLE mood_entry MODIFY note_shared TINYINT(1) NOT NULL DEFAULT 1,
 ADD recording_key VARCHAR(50) NULL,
 ADD recording_name VARCHAR(150) NULL,
 ADD recording_mime VARCHAR(60) NULL,
 ADD recording_size BIGINT NULL;
