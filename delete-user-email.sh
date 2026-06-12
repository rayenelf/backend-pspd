#!/bin/bash

# Script pour supprimer un email spécifique de la base de données
# Usage: ./delete-user-email.sh

EMAIL="mohamedrayen.elfidha@gmail.com"
BASE_URL="http://localhost:8080/api/auth"

echo "=== Suppression de l'email: $EMAIL ==="
echo

# Fonction pour faire un appel API DELETE
delete_pending_registration() {
    local email_encoded=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$EMAIL', safe=''))")
    
    echo "Suppression de la demande d'inscription en attente..."
    
    response=$(curl -s -w "\n%{http_code}" -X DELETE \
        "$BASE_URL/pending-registration/$email_encoded" \
        -H "Content-Type: application/json")
    
    body=$(echo "$response" | sed '$d')
    status_code=$(echo "$response" | tail -n1)
    
    echo "Status: $status_code"
    echo "Réponse: $body"
    echo
    
    if [ "$status_code" = "200" ]; then
        echo "✅ Demande d'inscription supprimée avec succès"
        return 0
    else
        echo "❌ Erreur lors de la suppression"
        return 1
    fi
}

# Vérifier que le serveur est en cours d'exécution
echo "Vérification de la connectivité au serveur..."
if ! curl -s "$BASE_URL/health" > /dev/null 2>&1; then
    echo "❌ Le serveur backend n'est pas accessible sur $BASE_URL"
    echo "Assurez-vous que le serveur est démarré avec ./run-dev.sh"
    exit 1
fi

echo "✅ Serveur accessible"
echo

# Supprimer la demande d'inscription
delete_pending_registration

echo "=== Fin du script ==="