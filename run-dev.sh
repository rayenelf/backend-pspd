#!/bin/bash
# ============================================================
# Lance le backend Spring Boot en chargeant les variables .env
# Usage : ./run-dev.sh
# ============================================================

ENV_FILE="$(dirname "$0")/../.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Fichier .env introuvable !"
  echo "   Crée-le : cp .env.example .env  puis remplis DB_PASSWORD"
  exit 1
fi

# Charger les variables
set -a
source "$ENV_FILE"
set +a

echo "✅ Variables chargées depuis .env"
echo "   DB_USERNAME=$DB_USERNAME"
echo "   DB_PASSWORD=****"
echo ""
echo "🚀 Démarrage Spring Boot..."
./mvnw spring-boot:run
