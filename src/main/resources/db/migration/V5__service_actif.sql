-- Sprint 2 (Epic B) — désactivation logique des services.
-- Permet à l'admin de retirer un service du catalogue (DELETE = soft delete)
-- sans casser les références (prestataire_services, réservations futures).
ALTER TABLE services
  ADD COLUMN actif TINYINT(1) NOT NULL DEFAULT 1;
