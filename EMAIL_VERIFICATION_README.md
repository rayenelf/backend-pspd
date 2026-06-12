# Vérification d'Email par OTP

Ce document décrit le nouveau processus d'inscription avec vérification d'email par code OTP.

## 🎯 Objectif

Implémenter la vérification d'email avant la création du compte utilisateur pour s'assurer que l'adresse email est valide et appartient à l'utilisateur.

## 🔄 Flux d'inscription

### 1. Ancien flux (temporairement maintenu)
```
POST /api/auth/register → Compte créé immédiatement
```

### 2. Nouveau flux (recommandé)
```
POST /api/auth/send-verification → OTP envoyé par email
POST /api/auth/verify-email → Code vérifié + compte créé
```

## 📡 Nouveaux Endpoints

### 1. Envoi du code de vérification
```http
POST /api/auth/send-verification
Content-Type: application/json

{
  "role": "CLIENT",
  "type": "PARTICULIER",
  "nom": "Ben Ali",
  "prenom": "Sara",
  "email": "sara@example.com",
  "telephone": "+21620123456",
  "motDePasse": "motDePasseSecurise123"
}
```

**Réponse (200):**
```json
{
  "success": true,
  "message": "Code de vérification envoyé avec succès",
  "email": "sara@example.com"
}
```

### 2. Vérification du code et création du compte
```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "sara@example.com",
  "code": "123456"
}
```

**Réponse (200):**
```json
{
  "id": "8f3a4b5c-...",
  "email": "sara@example.com",
  "role": "CLIENT",
  "statutCompte": "ACTIF"
}
```

### 3. Renvoyer un nouveau code
```http
POST /api/auth/resend-verification
Content-Type: application/json

{
  "email": "sara@example.com"
}
```

## 🗄️ Modifications Base de Données

### Table `otp_codes` (modifiée)
- Ajout du champ `email` pour supporter les OTP sans utilisateur existant
- Ajout du champ `otp_type` (`TWO_FACTOR_AUTH` | `EMAIL_VERIFICATION`)
- `user_id` devient nullable pour la vérification d'email

### Table `pending_registrations` (nouvelle)
- Stocke temporairement les données d'inscription en attente de vérification
- Expire automatiquement après 24h
- Supprimée après création du compte ou expiration

## 🔒 Sécurité

- **Codes OTP:** 6 chiffres, valides 10 minutes, 3 tentatives max
- **Hachage:** SHA-256 des codes OTP stockés en base
- **Expiration:** Demandes d'inscription expirent après 24h
- **Validation:** Vérification de l'unicité de l'email à chaque étape

## 📝 Logs de Développement

En mode développement, les codes OTP sont loggés dans la console :
```
[EMAIL VERIFICATION STUB] Code OTP pour sara@example.com : 123456
```

## 🚀 Migration en Production

En Phase 2, remplacer le stub par un vrai service d'email (SendGrid, SMTP, etc.)

## ❌ Gestion d'Erreurs

| Erreur | Code HTTP | Message |
|--------|-----------|---------|
| Email existant | 409 | Un compte existe déjà pour cet email |
| Code invalide | 400 | Code de vérification incorrect |
| Code expiré | 400 | Le code de vérification a expiré |
| Trop de tentatives | 400 | Trop de tentatives. Veuillez demander un nouveau code |
| Pas de demande en attente | 404 | Aucune demande d'inscription en attente pour cet email |

## 🧪 Test des Endpoints

### Avec curl :
```bash
# 1. Envoyer le code de vérification
curl -X POST http://localhost:8080/api/auth/send-verification \
  -H "Content-Type: application/json" \
  -d '{
    "role": "CLIENT",
    "type": "PARTICULIER", 
    "nom": "Test",
    "prenom": "User",
    "email": "test@example.com",
    "telephone": "+21620123456",
    "motDePasse": "password123"
  }'

# 2. Vérifier le code (récupéré dans les logs)
curl -X POST http://localhost:8080/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "123456"
  }'
```

## 🔧 Configuration

Aucune configuration supplémentaire nécessaire. Le système utilise la configuration existante de Spring Boot et MySQL.