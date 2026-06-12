-- Vérification d'email (auth avancé — Majd).
-- Nouvelle colonne sur users + table de tokens de vérification.

ALTER TABLE users ADD COLUMN email_verifie TINYINT(1) NOT NULL DEFAULT 0;

-- Comptes existants : considérés vérifiés (grandfathering) pour ne pas les bloquer.
UPDATE users SET email_verifie = 1;

CREATE TABLE email_verification_tokens (
  id         CHAR(36)     NOT NULL,
  user_id    CHAR(36)     NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME     NOT NULL,
  used       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_evt_user (user_id),
  INDEX idx_evt_token (token_hash),
  CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
