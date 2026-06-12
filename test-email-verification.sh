#!/bin/bash

# Script de test pour la vérification d'email par OTP
# Usage: ./test-email-verification.sh

set -e

BASE_URL="http://localhost:8081"
EMAIL="test-$(date +%s)@example.com"
PASSWORD="testPassword123"

echo "🚀 Test de la vérification d'email par OTP"
echo "Email de test: $EMAIL"
echo ""

# 1. Étape 1 : Envoyer le code de vérification
echo "📧 Étape 1: Envoi du code de vérification..."
SEND_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/send-verification" \
  -H "Content-Type: application/json" \
  -d "{
    \"role\": \"CLIENT\",
    \"type\": \"PARTICULIER\",
    \"nom\": \"Test\",
    \"prenom\": \"User\",
    \"email\": \"$EMAIL\",
    \"telephone\": \"+21620123456\",
    \"motDePasse\": \"$PASSWORD\"
  }")

echo "Réponse: $SEND_RESPONSE"

# Vérifier si la réponse contient "success": true
if echo "$SEND_RESPONSE" | grep -q '"success":true'; then
    echo "✅ Code de vérification envoyé avec succès"
else
    echo "❌ Erreur lors de l'envoi du code de vérification"
    exit 1
fi

echo ""
echo "💡 Consultez les logs du backend pour récupérer le code OTP"
echo "   Cherchez une ligne comme: [EMAIL VERIFICATION STUB] Code OTP pour $EMAIL : 123456"
echo ""
echo "📝 Pour continuer le test manuellement, utilisez ce code avec la commande suivante:"
echo ""
echo "curl -X POST \"$BASE_URL/api/auth/verify-email\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{"
echo "    \"email\": \"$EMAIL\","
echo "    \"code\": \"VOTRE_CODE_ICI\""
echo "  }'"
echo ""

# 2. Test avec un code invalide
echo "🔍 Test avec un code invalide..."
VERIFY_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/verify-email" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"code\": \"000000\"
  }" \
  || echo '{"error": "request failed"}')

echo "Réponse: $VERIFY_RESPONSE"

if echo "$VERIFY_RESPONSE" | grep -q 'incorrect\|invalide\|INVALID'; then
    echo "✅ Code invalide correctement rejeté"
else
    echo "⚠️  Réponse inattendue pour code invalide"
fi

echo ""

# 3. Test de renvoi de code
echo "🔄 Test de renvoi de code..."
RESEND_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/resend-verification" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\"
  }")

echo "Réponse: $RESEND_RESPONSE"

if echo "$RESEND_RESPONSE" | grep -q '"success":true'; then
    echo "✅ Nouveau code envoyé avec succès"
else
    echo "❌ Erreur lors du renvoi du code"
fi

echo ""
echo "🎯 Test terminé. Pour un test complet:"
echo "1. Récupérez le code OTP dans les logs du backend"
echo "2. Utilisez la commande curl ci-dessus avec le bon code"
echo "3. Vérifiez que le compte est créé avec succès"