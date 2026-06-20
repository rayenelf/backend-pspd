-- Jeu de données de test (dev) : comptes de tous types + prestataires dans tous
-- les statuts de validation + documents + favoris. Mot de passe commun : password123
-- (En prod, retirer cette migration.)

-- ── Utilisateurs ─────────────────────────────────────────────────────────────
-- Hash bcrypt de "password123"
INSERT INTO users (id, email, mot_de_passe_hash, telephone, nom, prenom, role, statut_compte, double_auth_active, email_verifie, cree_le) VALUES
 ('40000000-0000-0000-0000-000000000001', 'client.particulier@demo.pspd', '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000001', 'Bennani',  'Sara',  'CLIENT',      'ACTIF', 0, 1, NOW()),
 ('40000000-0000-0000-0000-000000000002', 'client.entreprise@demo.pspd',  '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000002', 'Atlas',    'Société', 'CLIENT',    'ACTIF', 0, 1, NOW()),
 ('40000000-0000-0000-0000-000000000003', 'client.2fa@demo.pspd',         '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000003', 'Cherif',   'Ahmed', 'CLIENT',      'ACTIF', 1, 1, NOW()),
 ('40000000-0000-0000-0000-000000000004', 'admin.demo@demo.pspd',         '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000004', 'Admin',    'Demo',  'ADMIN',       'ACTIF', 0, 1, NOW()),
 ('40000000-0000-0000-0000-000000000005', 'pro.attente1@demo.pspd',       '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000005', 'Slimani',  'Tarek', 'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('40000000-0000-0000-0000-000000000006', 'pro.attente2@demo.pspd',       '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000006', 'Hamdi',    'Yassine','PRESTATAIRE','ACTIF', 0, 1, NOW()),
 ('40000000-0000-0000-0000-000000000007', 'pro.verification@demo.pspd',   '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000007', 'Riahi',    'Fatma', 'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('40000000-0000-0000-0000-000000000008', 'pro.suspendu@demo.pspd',       '$2a$10$4ZObE.yQ6XTwi9hkqBo/kuZFG1D1GBir62Pw/xp0.cE7nTP9qc01e', '0620000008', 'Jelassi',  'Omar',  'PRESTATAIRE', 'SUSPENDU', 0, 1, NOW());

-- ── Clients (particulier / entreprise) ───────────────────────────────────────
INSERT INTO clients (user_id, type, raison_sociale, matricule_fiscal) VALUES
 ('40000000-0000-0000-0000-000000000001', 'PARTICULIER', NULL, NULL),
 ('40000000-0000-0000-0000-000000000002', 'ENTREPRISE',  'Société Atlas SARL', '1234567P'),
 ('40000000-0000-0000-0000-000000000003', 'PARTICULIER', NULL, NULL);

-- ── Prestataires dans tous les statuts de validation ─────────────────────────
INSERT INTO prestataires (user_id, nom_commercial, categorie_principale, zone_intervention, rayon_km, statut_validation, certifie, note_moyenne, langues, latitude, longitude) VALUES
 ('40000000-0000-0000-0000-000000000005', 'Plomberie Express', 'Plomberie',    'Tunis',    20, 'EN_ATTENTE',    0, 0.00, 'Arabe, Français', 36.810000, 10.170000),
 ('40000000-0000-0000-0000-000000000006', 'Élec Rapide',       'Électricité',  'Ariana',   15, 'EN_ATTENTE',    0, 0.00, 'Arabe',           36.860000, 10.190000),
 ('40000000-0000-0000-0000-000000000007', 'Clim Confort',      'Climatisation','La Marsa', 25, 'VERIFICATION',  0, 0.00, 'Arabe, Français', 36.880000, 10.320000),
 ('40000000-0000-0000-0000-000000000008', 'Vieux Pro',         'Ménage',       'Tunis',    20, 'SUSPENDU',      0, 3.20, 'Arabe',           36.800000, 10.180000);

-- Quelques services proposés (pour cohérence, même si non-VALIDE n'apparaît pas en recherche)
INSERT INTO prestataire_services (prestataire_id, service_id) VALUES
 ('40000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001'),
 ('40000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000005');

-- ── Documents légaux (à valider par l'admin) ─────────────────────────────────
INSERT INTO documents_legaux (id, prestataire_id, type, url_fichier, statut, verifie_le) VALUES
 ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000005', 'CIN',          '/uploads/demo-cin-1.pdf',         'EN_ATTENTE', NULL),
 ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000005', 'PATENTE_RC',   '/uploads/demo-patente-1.pdf',     'EN_ATTENTE', NULL),
 ('50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000006', 'CIN',          '/uploads/demo-cin-2.pdf',         'EN_ATTENTE', NULL),
 ('50000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000007', 'ASSURANCE_RC', '/uploads/demo-assurance-3.pdf',   'EN_ATTENTE', NULL),
 ('50000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000007', 'DIPLOME',      '/uploads/demo-diplome-3.pdf',     'VALIDE',     NOW());

-- ── Favoris (client → prestataire validé) ────────────────────────────────────
INSERT INTO favoris (client_id, prestataire_id, ajoute_le) VALUES
 ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', NOW()),
 ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000003', NOW());
