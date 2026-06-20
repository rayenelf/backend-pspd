-- ============================================================
-- V11__seed_valid_prestataires.sql — Comptes prestataires VALIDÉS (dev)
-- Le seed V9 ne contenait que des prestataires non validés ; depuis V10 le
-- login bloque tout prestataire dont le statut != VALIDE. On ajoute donc :
--   • un prestataire INDIVIDUEL validé   → pro.valide@demo.pspd
--   • une SOCIÉTÉ prestataire validée     → pro.societe@demo.pspd
-- Mot de passe commun : password123 (même hash bcrypt que V9).
-- ============================================================

INSERT INTO users
  (id, email, mot_de_passe_hash, telephone, nom, prenom, adresse, role,
   statut_compte, double_auth_active, email_verifie, cgu_acceptees, consentement_le, cree_le)
VALUES
 ('40000000-0000-0000-0000-000000000009', 'pro.valide@demo.pspd',
  '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e',
  '0620000009', 'Khelifi', 'Nizar', '15 Av. Habib Bourguiba, Tunis',
  'PRESTATAIRE', 'ACTIF', 0, 1, 1, NOW(), NOW()),
 ('40000000-0000-0000-0000-000000000010', 'pro.societe@demo.pspd',
  '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e',
  '0620000010', 'Atlas Services', 'SARL', 'Zone Industrielle, Ariana',
  'PRESTATAIRE', 'ACTIF', 0, 1, 1, NOW(), NOW());

INSERT INTO prestataires
  (user_id, nom_commercial, categorie_principale, zone_intervention, rayon_km,
   statut_validation, type_prestataire, certifie, note_moyenne, langues, latitude, longitude)
VALUES
 ('40000000-0000-0000-0000-000000000009', 'Bricolage Pro', 'Bricolage', 'Tunis', 20,
  'VALIDE', 'INDIVIDUEL', 1, 4.50, 'Arabe, Français', 36.806500, 10.181500),
 ('40000000-0000-0000-0000-000000000010', 'Atlas Services SARL', 'Ménage', 'Ariana', 30,
  'VALIDE', 'SOCIETE', 1, 4.20, 'Arabe, Français, Anglais', 36.866700, 10.193400);
