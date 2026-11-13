-- Existing assignments stay private. No backfill of submission sharing.
ALTER TABLE assignment ADD peer_feedback TINYINT(1) NOT NULL DEFAULT 0, ADD peer_feedback_configured TINYINT(1) NOT NULL DEFAULT 0;
CREATE TABLE assignment_peer_feedback (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 assignment_id BIGINT NOT NULL,
 reviewer_id BIGINT NOT NULL,
 submission_id BIGINT NOT NULL,
 attempt_no INT NOT NULL,
 strength VARCHAR(500) NULL,
 suggestion VARCHAR(500) NULL,
 sent_at DATETIME NULL,
 hidden TINYINT(1) NOT NULL DEFAULT 0,
 hide_reason VARCHAR(300) NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_peer_reviewer(assignment_id,reviewer_id),
 KEY idx_peer_target(submission_id,attempt_no),
 FOREIGN KEY(assignment_id) REFERENCES assignment(id),
 FOREIGN KEY(reviewer_id) REFERENCES sys_user(id),
 FOREIGN KEY(submission_id,attempt_no) REFERENCES assignment_attempt(submission_id,attempt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
