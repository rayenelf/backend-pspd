-- ============================================================
-- V10__auth_hardening.sql — Durcissement de l'authentification
-- Cahier des charges §3, §4, §22.
--   • Adresse à l'inscription (client ET prestataire)            (§4)
--   • Consentement CGU / RGPD horodaté                           (§22)
--   • Distinction Prestataire Individuel / Société               (§3)
-- Les valeurs DEFAULT couvrent les lignes déjà présentes afin que
-- la validation Hibernate (ddl-auto=validate) reste satisfaite.
-- ============================================================

-- ── Adresse de l'utilisateur (champ unique du formulaire d'inscription) ──
ALTER TABLE users
  ADD COLUMN adresse VARCHAR(255) NULL AFTER prenom;

-- ── Consentement légal (§22 — RGPD / CGU) ──
ALTER TABLE users
  ADD COLUMN cgu_acceptees   TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN consentement_le DATETIME   NULL;

-- ── Type de prestataire : individuel (défaut) vs société (§3) ──
ALTER TABLE prestataires
  ADD COLUMN type_prestataire VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUEL';

ALTER TABLE prestataires
  ADD CONSTRAINT chk_prest_type CHECK (type_prestataire IN ('INDIVIDUEL','SOCIETE'));
