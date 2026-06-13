-- Aligne le type des coordonnées sur le mapping JPA (Double → DOUBLE).
-- DECIMAL(9,6) → DOUBLE : conversion des valeurs existantes sans perte significative.
ALTER TABLE prestataires
  MODIFY latitude  DOUBLE NULL,
  MODIFY longitude DOUBLE NULL;
