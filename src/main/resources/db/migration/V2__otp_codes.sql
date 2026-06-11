-- Table de stockage des OTP pour la double authentification (B7 — Majd).
-- Redis n'est disponible qu'en Phase 2 ; on stocke les codes en base pour Phase 1.
-- TTL géré applicativement via la colonne expires_at.
CREATE TABLE otp_codes (
  id         CHAR(36)     NOT NULL,
  user_id    CHAR(36)     NOT NULL,
  code_hash  VARCHAR(255) NOT NULL,
  expires_at DATETIME     NOT NULL,
  attempts   INT          NOT NULL DEFAULT 0,
  used       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_otp_user_id (user_id),
  CONSTRAINT fk_otp_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
