-- Migration pour la vérification d'email par OTP
-- Ajoute le support pour différents types d'OTP et les demandes d'inscription en attente

-- 1. Modifier la table otp_codes pour supporter la vérification d'email
ALTER TABLE otp_codes 
ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '',
ADD COLUMN otp_type ENUM('TWO_FACTOR_AUTH', 'EMAIL_VERIFICATION') NOT NULL DEFAULT 'TWO_FACTOR_AUTH';

-- Modifier la contrainte user_id pour permettre NULL (pour la vérification d'email)
ALTER TABLE otp_codes 
MODIFY COLUMN user_id CHAR(36) NULL;

-- Ajouter un index sur email et otp_type
ALTER TABLE otp_codes 
ADD INDEX idx_otp_email_type (email, otp_type);

-- Supprimer l'ancienne contrainte de clé étrangère et la recréer avec ON DELETE CASCADE
ALTER TABLE otp_codes DROP FOREIGN KEY fk_otp_user;
ALTER TABLE otp_codes 
ADD CONSTRAINT fk_otp_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 2. Créer la table pour les demandes d'inscription en attente
CREATE TABLE pending_registrations (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    role ENUM('CLIENT', 'PRESTATAIRE', 'ADMIN', 'SUPER_ADMIN') NOT NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    
    -- Champs spécifiques CLIENT
    type_client ENUM('PARTICULIER', 'ENTREPRISE') NULL,
    raison_sociale VARCHAR(255) NULL,
    matricule_fiscal VARCHAR(50) NULL,
    
    -- Champs spécifiques PRESTATAIRE
    nom_commercial VARCHAR(255) NULL,
    categorie_principale VARCHAR(255) NULL,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    
    PRIMARY KEY (id),
    INDEX idx_pending_email (email),
    INDEX idx_pending_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;