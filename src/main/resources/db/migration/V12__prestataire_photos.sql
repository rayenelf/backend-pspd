-- Photos du prestataire : avatar (photo de profil) + portfolio (réalisations).
-- Distinct des documents légaux (privés, vérifiés par l'admin) : ces photos sont
-- publiques et destinées à être affichées sur le profil public du prestataire.
--
-- NB : prestataires.user_id est CHAR(36) utf8mb4/utf8mb4_unicode_ci → la colonne
-- référençante DOIT avoir exactement le même type/charset (sinon FK 3780).

-- Avatar / photo de profil (chemin relatif type /uploads/<uuid>, NULL si absent).
ALTER TABLE prestataires
  ADD COLUMN photo_url VARCHAR(400) NULL;

-- Portfolio : 0..N photos de réalisations (limite applicative : 12).
CREATE TABLE photos_travail (
  id              CHAR(36)     NOT NULL,
  prestataire_id  CHAR(36)     NOT NULL,
  url_fichier     VARCHAR(400) NOT NULL,
  ordre           INT          NOT NULL DEFAULT 0,
  cree_le         DATETIME     NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_photos_travail_prestataire
    FOREIGN KEY (prestataire_id) REFERENCES prestataires (user_id)
    ON DELETE CASCADE,
  INDEX idx_photos_travail_prestataire (prestataire_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
