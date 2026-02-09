#!/bin/bash

# Script per testare il flusso completo di upload immagine e creazione content

echo "=== Test Upload Immagine e Collegamento Content ==="
echo ""

# Variabili (modifica questi valori)
API_URL="http://localhost:8080"
USERNAME="testuser"
PASSWORD="password123"
IMAGE_FILE="test.jpg"  # Assicurati che questo file esista

echo "1. Login e ottenimento token JWT..."
TOKEN_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Errore: Login fallito"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo "✅ Token ottenuto: ${TOKEN:0:20}..."
echo ""

echo "2. Upload immagine..."
UPLOAD_RESPONSE=$(curl -s -X POST "$API_URL/api/media" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@$IMAGE_FILE")

echo "Upload Response: $UPLOAD_RESPONSE"
echo ""

MEDIA_ID=$(echo $UPLOAD_RESPONSE | jq -r '.media.id')
MEDIA_URL=$(echo $UPLOAD_RESPONSE | jq -r '.media.fileUrl')

if [ "$MEDIA_ID" == "null" ] || [ -z "$MEDIA_ID" ]; then
    echo "❌ Errore: Upload fallito"
    exit 1
fi

echo "✅ Immagine caricata"
echo "   Media ID: $MEDIA_ID"
echo "   Media URL: $MEDIA_URL"
echo ""

echo "3. Verifica che il media esista..."
MEDIA_CHECK=$(curl -s "$API_URL/api/media/info/$MEDIA_ID")
echo "Media info: $MEDIA_CHECK"
echo ""

echo "4. Creazione content con featured image..."
SLUG="test-media-$(date +%s)"
CREATE_RESPONSE=$(curl -s -X POST "$API_URL/api/contents" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Test Content con Immagine\",
    \"slug\": \"$SLUG\",
    \"body\": \"Questo è un test per verificare il collegamento tra content e media\",
    \"excerpt\": \"Test excerpt\",
    \"featuredImageId\": $MEDIA_ID,
    \"status\": \"PUBLISHED\"
  }")

echo "Create Response: $CREATE_RESPONSE"
echo ""

CONTENT_ID=$(echo $CREATE_RESPONSE | jq -r '.id')

if [ "$CONTENT_ID" == "null" ] || [ -z "$CONTENT_ID" ]; then
    echo "❌ Errore: Creazione content fallita"
    exit 1
fi

echo "✅ Content creato"
echo "   Content ID: $CONTENT_ID"
echo "   Slug: $SLUG"
echo ""

echo "5. Recupero content tramite slug..."
GET_RESPONSE=$(curl -s "$API_URL/api/contents/$SLUG")

echo "Get Response (formatted):"
echo $GET_RESPONSE | jq '.'
echo ""

FEATURED_IMAGE=$(echo $GET_RESPONSE | jq '.featuredImage')
FEATURED_IMAGE_URL=$(echo $GET_RESPONSE | jq -r '.featuredImage.fileUrl')

echo "6. Verifica risultato..."
echo "   featuredImage object: $FEATURED_IMAGE"
echo "   featuredImage.fileUrl: $FEATURED_IMAGE_URL"
echo ""

if [ "$FEATURED_IMAGE" == "null" ] || [ "$FEATURED_IMAGE_URL" == "null" ]; then
    echo "❌ PROBLEMA: featuredImage è NULL nella response!"
    echo ""
    echo "Debug: Controlla i log dell'applicazione Spring Boot per vedere:"
    echo "  - Se il JOIN FETCH funziona"
    echo "  - Se featured_image_id è salvato nel database"
    echo ""
    echo "Puoi anche verificare nel database con:"
    echo "  SELECT id, slug, featured_image_id FROM contents WHERE slug = '$SLUG';"
    exit 1
else
    echo "✅ SUCCESS: featuredImage è presente nella response!"
    echo ""
    echo "Puoi visualizzare l'immagine a: $FEATURED_IMAGE_URL"
fi

echo ""
echo "=== Test completato ==="
