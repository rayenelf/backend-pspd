-- Géolocalisation des prestataires (B4 — recherche par distance / carte).
ALTER TABLE prestataires
  ADD COLUMN latitude  DECIMAL(9,6) NULL,
  ADD COLUMN longitude DECIMAL(9,6) NULL;

-- Index léger pour pré-filtrer les prestataires géolocalisés.
CREATE INDEX idx_prestataires_geo ON prestataires (latitude, longitude);
