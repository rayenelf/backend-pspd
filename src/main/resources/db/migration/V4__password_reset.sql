-- Réinitialisation de mot de passe (mot de passe oublié — Majd).
-- Token à usage unique, TTL court (1 h).
CREATE TABLE password_reset_tokens (
  id         CHAR(36)     NOT NULL,
  user_id    CHAR(36)     NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME     NOT NULL,
  used       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_prt_user (user_id),
  INDEX idx_prt_token (token_hash),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
