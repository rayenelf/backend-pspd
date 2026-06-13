-- Données de démonstration (Epic B) : catalogue + prestataires géolocalisés.
-- UUID fixes pour rester idempotent à la ré-exécution d'un environnement neuf.
-- (En prod, ce jeu de données peut être retiré.)

-- ── Catégories ───────────────────────────────────────────────────────────────
INSERT INTO categories (id, parent_id, libelle, slug, actif) VALUES
 ('10000000-0000-0000-0000-000000000001', NULL, 'Électricité', 'electricite', 1),
 ('10000000-0000-0000-0000-000000000002', NULL, 'Jardinage',   'jardinage',   1),
 ('10000000-0000-0000-0000-000000000003', NULL, 'Ménage',      'menage',      1);

-- ── Services ─────────────────────────────────────────────────────────────────
INSERT INTO services (id, categorie_id, libelle, description, prix_indicatif, unite) VALUES
 ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Installation électrique', 'Pose et mise aux normes',        80.00, 'forfait'),
 ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Dépannage électrique',    'Intervention rapide',            60.00, 'heure'),
 ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'Tonte de pelouse',        'Entretien jardin',               50.00, 'forfait'),
 ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'Taille de haies',         'Élagage et taille',              45.00, 'heure'),
 ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', 'Ménage complet',          'Nettoyage du domicile',          40.00, 'heure'),
 ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', 'Repassage',               'Repassage à domicile',           30.00, 'heure');

-- ── Utilisateurs prestataires (mot de passe = même hash de démo) ─────────────
INSERT INTO users (id, email, mot_de_passe_hash, telephone, nom, prenom, role, statut_compte, double_auth_active, email_verifie, cree_le) VALUES
 ('30000000-0000-0000-0000-000000000001', 'elecpro@demo.pspd',   '$2a$10$xt/R/u7i4IaACKbki6Rm5.Sx4KCYMS13v7ExPWvI3DcQFU4smL.EC', '0610000001', 'Ben Ali',  'Karim',  'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('30000000-0000-0000-0000-000000000002', 'voltexpress@demo.pspd','$2a$10$xt/R/u7i4IaACKbki6Rm5.Sx4KCYMS13v7ExPWvI3DcQFU4smL.EC', '0610000002', 'Trabelsi','Salma',  'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('30000000-0000-0000-0000-000000000003', 'jardinvert@demo.pspd', '$2a$10$xt/R/u7i4IaACKbki6Rm5.Sx4KCYMS13v7ExPWvI3DcQFU4smL.EC', '0610000003', 'Khelifi', 'Mehdi',  'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('30000000-0000-0000-0000-000000000004', 'greengarden@demo.pspd','$2a$10$xt/R/u7i4IaACKbki6Rm5.Sx4KCYMS13v7ExPWvI3DcQFU4smL.EC', '0610000004', 'Gharbi',  'Sami',   'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('30000000-0000-0000-0000-000000000005', 'cleanhome@demo.pspd',  '$2a$10$xt/R/u7i4IaACKbki6Rm5.Sx4KCYMS13v7ExPWvI3DcQFU4smL.EC', '0610000005', 'Mansour', 'Leila',  'PRESTATAIRE', 'ACTIF', 0, 1, NOW()),
 ('30000000-0000-0000-0000-000000000006', 'netplus@demo.pspd',    '$2a$10$xt/R/u7i4IaACKbki6Rm5.Sx4KCYMS13v7ExPWvI3DcQFU4smL.EC', '0610000006', 'Ayari',   'Nizar',  'PRESTATAIRE', 'ACTIF', 0, 1, NOW());

-- ── Profils prestataires (VALIDE, géolocalisés autour de Tunis) ──────────────
INSERT INTO prestataires (user_id, nom_commercial, categorie_principale, zone_intervention, rayon_km, statut_validation, certifie, note_moyenne, langues, latitude, longitude) VALUES
 ('30000000-0000-0000-0000-000000000001', 'ElecPro Tunis', 'Électricité', 'Tunis',     20, 'VALIDE', 1, 4.80, 'Arabe, Français',          36.806500, 10.181500),
 ('30000000-0000-0000-0000-000000000002', 'Volt Express',  'Électricité', 'La Marsa',  15, 'VALIDE', 0, 4.50, 'Arabe, Français, Anglais', 36.878000, 10.324000),
 ('30000000-0000-0000-0000-000000000003', 'Jardin Vert',   'Jardinage',   'Ariana',    25, 'VALIDE', 1, 4.90, 'Arabe, Français',          36.866000, 10.193000),
 ('30000000-0000-0000-0000-000000000004', 'Green Garden',  'Jardinage',   'Ben Arous', 20, 'VALIDE', 0, 4.20, 'Arabe',                    36.753000, 10.231000),
 ('30000000-0000-0000-0000-000000000005', 'CleanHome',     'Ménage',      'Tunis',     30, 'VALIDE', 1, 4.70, 'Arabe, Français',          36.800000, 10.180000),
 ('30000000-0000-0000-0000-000000000006', 'NetPlus',       'Ménage',      'Manouba',   20, 'VALIDE', 0, 4.00, 'Arabe, Français',          36.808000, 10.097000);

-- ── Liens prestataire ↔ services ─────────────────────────────────────────────
INSERT INTO prestataire_services (prestataire_id, service_id) VALUES
 ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'),
 ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002'),
 ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002'),
 ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003'),
 ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004'),
 ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003'),
 ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005'),
 ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000006'),
 ('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000005');
