-- ============================================================================
-- Sprint 3 — Epic C : Réservation
-- Table `reservations` (cycle de vie : 12-state-reservation.png)
-- + table `notifications` minimale (notification client à chaque transition).
-- Réf. 02-modele-de-donnees §reservations.
-- ============================================================================

CREATE TABLE reservations (
  id             CHAR(36)     NOT NULL,
  client_id      CHAR(36)     NOT NULL,
  prestataire_id CHAR(36)     NOT NULL,
  service_id     CHAR(36)     NOT NULL,
  adresse_id     CHAR(36)     NOT NULL,
  type           VARCHAR(20)  NOT NULL,
  statut         VARCHAR(20)  NOT NULL,
  date_service   DATE         NOT NULL,
  heure_service  TIME         NOT NULL,
  prix_convenu   DECIMAL(12,2),
  cree_le        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  maj_le         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_resa_client      FOREIGN KEY (client_id)      REFERENCES users(id),
  CONSTRAINT fk_resa_prestataire FOREIGN KEY (prestataire_id) REFERENCES users(id),
  CONSTRAINT fk_resa_service     FOREIGN KEY (service_id)     REFERENCES services(id),
  CONSTRAINT fk_resa_adresse     FOREIGN KEY (adresse_id)     REFERENCES adresses(id),
  CONSTRAINT chk_resa_type   CHECK (type   IN ('IMMEDIATE','AVEC_DEVIS')),
  CONSTRAINT chk_resa_statut CHECK (statut IN ('EN_ATTENTE','ACCEPTEE','REFUSEE','EN_COURS','TERMINEE','ANNULEE','EN_LITIGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_resa_client      ON reservations (client_id);
CREATE INDEX idx_resa_prestataire ON reservations (prestataire_id);
CREATE INDEX idx_resa_statut      ON reservations (statut);
CREATE INDEX idx_resa_creneau     ON reservations (date_service, heure_service);

-- ── Notifications (version minimale Sprint 3 — étendue Phase 2 / Epic E) ──────
CREATE TABLE notifications (
  id         CHAR(36)     NOT NULL,
  user_id    CHAR(36)     NOT NULL,
  type       VARCHAR(40)  NOT NULL,
  payload    JSON,
  lu         TINYINT(1)   NOT NULL DEFAULT 0,
  cree_le    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notif_user_lu ON notifications (user_id, lu);
