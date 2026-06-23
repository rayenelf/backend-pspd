-- Slug URL-friendly pour les pages publiques des prestataires (/prestataires/elecpro-tunis).
ALTER TABLE prestataires ADD COLUMN slug VARCHAR(140) NULL;

-- Initialisation depuis nom_commercial : minuscules, alphanumérique → tirets, trim.
UPDATE prestataires
SET slug = LOWER(
             TRIM(BOTH '-' FROM
               REGEXP_REPLACE(
                 REGEXP_REPLACE(nom_commercial, '[^a-zA-Z0-9 -]', ''),
                 '[ -]+', '-'
               )
             )
           )
WHERE slug IS NULL OR slug = '';

-- Résolution des doublons éventuels : suffixe avec le début de l'user_id.
-- ROW_NUMBER() est compatible avec le mode SQL only_full_group_by de MySQL 8.
UPDATE prestataires p
JOIN (
  SELECT user_id,
         slug,
         ROW_NUMBER() OVER (PARTITION BY slug ORDER BY user_id) AS rn
  FROM prestataires
) ranked ON p.user_id = ranked.user_id
SET p.slug = CONCAT(ranked.slug, '-', LEFT(p.user_id, 8))
WHERE ranked.rn > 1;

-- Index unique (après peuplement pour éviter les collisions).
CREATE UNIQUE INDEX uq_prestataires_slug ON prestataires (slug);
