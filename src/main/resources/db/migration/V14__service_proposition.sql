-- Permet à un prestataire de proposer un nouveau service, soumis à validation admin.
-- statut : APPROUVE (visible au catalogue public) ou EN_ATTENTE (proposé, en attente).
-- propose_par : user_id du prestataire à l'origine de la proposition (NULL pour le seed admin).
ALTER TABLE services
  ADD COLUMN statut      VARCHAR(20) NOT NULL DEFAULT 'APPROUVE',
  ADD COLUMN propose_par CHAR(36)    NULL;

-- Les services existants du catalogue restent approuvés (valeur par défaut déjà appliquée).
