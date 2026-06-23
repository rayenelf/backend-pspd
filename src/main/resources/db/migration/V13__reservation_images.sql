-- ============================================================
-- V13 — Images de travail jointes à une réservation (flow AVEC_DEVIS)
--
-- Le client joint des photos du travail à réaliser au moment de créer
-- une réservation AVEC_DEVIS ; le prestataire les consulte avant de
-- chiffrer son devis (la table `devis` existe déjà depuis V1__init).
-- Fichiers stockés sur disque via FileStorageService ; seule l'URL
-- relative (ex. /uploads/xxx.jpg) est conservée en base.
-- ============================================================

CREATE TABLE reservation_images (
  id             CHAR(36)     NOT NULL,
  reservation_id CHAR(36)     NOT NULL,
  url            VARCHAR(400) NOT NULL,
  content_type   VARCHAR(100),
  ordre          INT          NOT NULL DEFAULT 0,
  cree_le        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_resv_images_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_resv_images_reservation ON reservation_images(reservation_id);
