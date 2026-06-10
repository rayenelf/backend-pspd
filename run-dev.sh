#!/bin/bash
# ============================================================
# Lance le backend Spring Boot en chargeant les variables .env
# Prérequis : docker compose up -d  (MySQL doit tourner)
# Usage     : ./run-dev.sh
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
echo "   DB_HOST=$DB_HOST  DB_NAME=$DB_NAME  DB_USERNAME=$DB_USERNAME"
echo "   DB_PASSWORD=****"
echo ""

# Vérifier que MySQL Docker est accessible
echo "⏳ Vérification de MySQL..."
for i in {1..10}; do
  if docker exec pspd_mysql mysqladmin ping -h localhost -u root -p"$DB_PASSWORD" --silent 2>/dev/null; then
    echo "✅ MySQL prêt !"
    break
  fi
  if [ $i -eq 10 ]; then
    echo "⚠️  MySQL inaccessible. Assure-toi que Docker tourne : docker compose up -d"
  fi
  sleep 2
done

echo ""
echo "🚀 Démarrage Spring Boot..."
./mvnw spring-boot:run
