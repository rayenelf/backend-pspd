-- ============================================================
-- V1__init.sql — Schéma initial PSPD
-- Base : MySQL 8.4 | Encodage : utf8mb4
-- UUID : CHAR(36) généré côté Java (UUID.randomUUID().toString())
-- Géolocalisation : colonnes latitude/longitude DOUBLE
--   + formule Haversine en JPQL pour la recherche par rayon
-- ============================================================

-- ============ UTILISATEURS ============
CREATE TABLE users (
  id                 CHAR(36)     NOT NULL,
  email              VARCHAR(190) NOT NULL,
  mot_de_passe_hash  VARCHAR(255),
  telephone          VARCHAR(30)  NOT NULL,
  nom                VARCHAR(120),
  prenom             VARCHAR(120),
  role               VARCHAR(20)  NOT NULL,
  statut_compte      VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
  double_auth_active TINYINT(1)   NOT NULL DEFAULT 0,
  cree_le            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  CONSTRAINT chk_users_role        CHECK (role IN ('CLIENT','PRESTATAIRE','ADMIN','SUPER_ADMIN')),
  CONSTRAINT chk_users_statut      CHECK (statut_compte IN ('ACTIF','SUSPENDU','SUPPRIME'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clients (
  user_id          CHAR(36)     NOT NULL,
  type             VARCHAR(20)  NOT NULL,
  raison_sociale   VARCHAR(180),
  matricule_fiscal VARCHAR(60),
  PRIMARY KEY (user_id),
  CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_clients_type CHECK (type IN ('PARTICULIER','ENTREPRISE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE prestataires (
  user_id              CHAR(36)     NOT NULL,
  nom_commercial       VARCHAR(180) NOT NULL,
  categorie_principale VARCHAR(80),
  zone_intervention    VARCHAR(180),
  rayon_km             INT          NOT NULL DEFAULT 10,
  statut_validation    VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
  certifie             TINYINT(1)   NOT NULL DEFAULT 0,
  note_moyenne         DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  langues              VARCHAR(120),
  PRIMARY KEY (user_id),
  CONSTRAINT fk_prestataires_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_prest_statut       CHECK (statut_validation IN ('EN_ATTENTE','VERIFICATION','VALIDE','SUSPENDU'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE adresses (
  id          CHAR(36)     NOT NULL,
  user_id     CHAR(36),
  rue         VARCHAR(180),
  ville       VARCHAR(120),
  code_postal VARCHAR(20),
  latitude    DOUBLE,
  longitude   DOUBLE,
  PRIMARY KEY (id),
  CONSTRAINT fk_adresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE documents_legaux (
  id             CHAR(36)     NOT NULL,
  prestataire_id CHAR(36)     NOT NULL,
  type           VARCHAR(30)  NOT NULL,
  url_fichier    VARCHAR(400) NOT NULL,
  statut         VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
  verifie_le     DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT fk_docs_prestataire FOREIGN KEY (prestataire_id) REFERENCES prestataires(user_id) ON DELETE CASCADE,
  CONSTRAINT chk_docs_type   CHECK (type   IN ('CIN','PATENTE_RC','ATTESTATION_FISCALE','ASSURANCE_RC','DIPLOME')),
  CONSTRAINT chk_docs_statut CHECK (statut IN ('EN_ATTENTE','VERIFICATION','VALIDE','SUSPENDU'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ CATALOGUE ============
CREATE TABLE categories (
  id        CHAR(36)     NOT NULL,
  parent_id CHAR(36),
  libelle   VARCHAR(120) NOT NULL,
  slug      VARCHAR(120) NOT NULL,
  actif     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_slug (slug),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE services (
  id             CHAR(36)     NOT NULL,
  categorie_id   CHAR(36)     NOT NULL,
  libelle        VARCHAR(160) NOT NULL,
  description    TEXT,
  prix_indicatif DECIMAL(12,2),
  unite          VARCHAR(40),
  PRIMARY KEY (id),
  CONSTRAINT fk_services_categorie FOREIGN KEY (categorie_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE prestataire_services (
  prestataire_id CHAR(36) NOT NULL,
  service_id     CHAR(36) NOT NULL,
  PRIMARY KEY (prestataire_id, service_id),
  CONSTRAINT fk_ps_prestataire FOREIGN KEY (prestataire_id) REFERENCES prestataires(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_ps_service     FOREIGN KEY (service_id)     REFERENCES services(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disponibilites (
  id             CHAR(36)    NOT NULL,
  prestataire_id CHAR(36)    NOT NULL,
  jour           VARCHAR(10),
  heure_debut    TIME,
  heure_fin      TIME,
  PRIMARY KEY (id),
  CONSTRAINT fk_dispo_prestataire FOREIGN KEY (prestataire_id) REFERENCES prestataires(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ FINANCE (références) ============
CREATE TABLE commissions (
  id           CHAR(36)     NOT NULL,
  type         VARCHAR(20)  NOT NULL,
  taux         DECIMAL(5,2),
  montant_fixe DECIMAL(12,2),
  PRIMARY KEY (id),
  CONSTRAINT chk_comm_type CHECK (type IN ('POURCENTAGE','FIXE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ RESERVATION ============
CREATE TABLE reservations (
  id             CHAR(36)     NOT NULL,
  client_id      CHAR(36)     NOT NULL,
  prestataire_id CHAR(36)     NOT NULL,
  service_id     CHAR(36)     NOT NULL,
  adresse_id     CHAR(36),
  type           VARCHAR(20)  NOT NULL,
  statut         VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
  date_service   DATE,
  heure_service  TIME,
  description    TEXT,
  prix_convenu   DECIMAL(12,2),
  cree_le        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_resv_client      FOREIGN KEY (client_id)      REFERENCES users(id),
  CONSTRAINT fk_resv_prestataire FOREIGN KEY (prestataire_id) REFERENCES users(id),
  CONSTRAINT fk_resv_service     FOREIGN KEY (service_id)     REFERENCES services(id),
  CONSTRAINT fk_resv_adresse     FOREIGN KEY (adresse_id)     REFERENCES adresses(id),
  CONSTRAINT chk_resv_type   CHECK (type   IN ('IMMEDIATE','AVEC_DEVIS')),
  CONSTRAINT chk_resv_statut CHECK (statut IN ('EN_ATTENTE','ACCEPTEE','REFUSEE','EN_COURS','TERMINEE','ANNULEE','EN_LITIGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE devis (
  id              CHAR(36)     NOT NULL,
  reservation_id  CHAR(36)     NOT NULL,
  montant         DECIMAL(12,2),
  duree_estimee_h DECIMAL(5,2),
  conditions      TEXT,
  statut          VARCHAR(20)  NOT NULL DEFAULT 'ENVOYE',
  emis_le         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_devis_reservation (reservation_id),
  CONSTRAINT fk_devis_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  CONSTRAINT chk_devis_statut CHECK (statut IN ('ENVOYE','ACCEPTE','REFUSE','NEGOCIATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE paiements (
  id                CHAR(36)     NOT NULL,
  reservation_id    CHAR(36)     NOT NULL,
  commission_id     CHAR(36),
  montant           DECIMAL(12,2) NOT NULL,
  moyen             VARCHAR(20)  NOT NULL,
  statut            VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE',
  reference_externe VARCHAR(120),
  cree_le           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_paiements_reservation (reservation_id),
  CONSTRAINT fk_paiements_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
  CONSTRAINT fk_paiements_commission  FOREIGN KEY (commission_id)  REFERENCES commissions(id),
  CONSTRAINT chk_paiements_moyen  CHECK (moyen  IN ('CARTE','WALLET','VIREMENT','A_LA_PRESTATION')),
  CONSTRAINT chk_paiements_statut CHECK (statut IN ('EN_ATTENTE','SEQUESTRE','LIBERE','REMBOURSE','ECHEC'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE factures (
  id             CHAR(36)     NOT NULL,
  reservation_id CHAR(36)     NOT NULL,
  numero         VARCHAR(40)  NOT NULL,
  montant_ht     DECIMAL(12,2),
  tva            DECIMAL(12,2),
  montant_ttc    DECIMAL(12,2),
  emise_le       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_factures_numero (numero),
  CONSTRAINT fk_factures_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ QUALITE ============
CREATE TABLE avis (
  id                 CHAR(36)     NOT NULL,
  reservation_id     CHAR(36)     NOT NULL,
  note_qualite       INT,
  note_ponctualite   INT,
  note_communication INT,
  note_rapport_qp    INT,
  note_globale       DECIMAL(3,2),
  commentaire        TEXT,
  suspect_faux       TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_avis_reservation (reservation_id),
  CONSTRAINT fk_avis_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
  CONSTRAINT chk_avis_qualite        CHECK (note_qualite        BETWEEN 1 AND 5),
  CONSTRAINT chk_avis_ponctualite    CHECK (note_ponctualite    BETWEEN 1 AND 5),
  CONSTRAINT chk_avis_communication  CHECK (note_communication  BETWEEN 1 AND 5),
  CONSTRAINT chk_avis_rapport_qp     CHECK (note_rapport_qp     BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE litiges (
  id                CHAR(36)     NOT NULL,
  reservation_id    CHAR(36)     NOT NULL,
  admin_id          CHAR(36),
  type              VARCHAR(30)  NOT NULL,
  statut            VARCHAR(20)  NOT NULL DEFAULT 'RECLAMATION',
  description       TEXT,
  decision          TEXT,
  montant_rembourse DECIMAL(12,2),
  PRIMARY KEY (id),
  CONSTRAINT fk_litiges_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
  CONSTRAINT fk_litiges_admin       FOREIGN KEY (admin_id)       REFERENCES users(id),
  CONSTRAINT chk_litiges_type   CHECK (type   IN ('PRESTATAIRE_ABSENT','NON_CONFORME','ANNULATION','RETARD')),
  CONSTRAINT chk_litiges_statut CHECK (statut IN ('RECLAMATION','ANALYSE','MEDIATION','DECISION','CLOTURE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ COMMUNICATION ============
CREATE TABLE conversations (
  id             CHAR(36) NOT NULL,
  reservation_id CHAR(36),
  cree_le        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_conv_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
  id               CHAR(36)     NOT NULL,
  conversation_id  CHAR(36)     NOT NULL,
  auteur_id        CHAR(36)     NOT NULL,
  contenu          TEXT,
  piece_jointe_url VARCHAR(400),
  lu               TINYINT(1)   NOT NULL DEFAULT 0,
  envoye_le        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_auteur       FOREIGN KEY (auteur_id)       REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
  id              CHAR(36)     NOT NULL,
  destinataire_id CHAR(36)     NOT NULL,
  canal           VARCHAR(10)  NOT NULL,
  titre           VARCHAR(160),
  contenu         TEXT,
  lu              TINYINT(1)   NOT NULL DEFAULT 0,
  envoyee_le      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_notif_destinataire FOREIGN KEY (destinataire_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_notif_canal CHECK (canal IN ('PUSH','SMS','EMAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favoris (
  client_id      CHAR(36) NOT NULL,
  prestataire_id CHAR(36) NOT NULL,
  ajoute_le      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (client_id, prestataire_id),
  CONSTRAINT fk_favoris_client      FOREIGN KEY (client_id)      REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_favoris_prestataire FOREIGN KEY (prestataire_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE abonnements (
  id             CHAR(36)    NOT NULL,
  prestataire_id CHAR(36)    NOT NULL,
  plan           VARCHAR(20) NOT NULL,
  debut          DATE,
  fin            DATE,
  actif          TINYINT(1)  NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT fk_abo_prestataire FOREIGN KEY (prestataire_id) REFERENCES prestataires(user_id) ON DELETE CASCADE,
  CONSTRAINT chk_abo_plan CHECK (plan IN ('GRATUIT','PREMIUM','MISE_EN_AVANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ INDEX ============
CREATE INDEX idx_resv_client       ON reservations(client_id);
CREATE INDEX idx_resv_prestataire  ON reservations(prestataire_id);
CREATE INDEX idx_resv_statut       ON reservations(statut);
CREATE INDEX idx_paiements_statut  ON paiements(statut);
CREATE INDEX idx_services_cat      ON services(categorie_id);
CREATE INDEX idx_msg_conv          ON messages(conversation_id);
CREATE INDEX idx_notif_dest        ON notifications(destinataire_id);
-- Index géo : latitude + longitude pour la formule Haversine
CREATE INDEX idx_adresses_lat      ON adresses(latitude);
CREATE INDEX idx_adresses_lng      ON adresses(longitude);
