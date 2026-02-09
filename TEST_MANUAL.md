# Test Manuale - Collegamento Media e Content

Segui questi passi per testare il collegamento tra immagini e content.

## Setup Iniziale

**Riavvia l'applicazione** per applicare tutte le modifiche:
```bash
gradlew.bat bootRun
```

## Step 1: Login

```bash
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"TUOUSERNAME\",\"password\":\"TUAPASSWORD\"}"
```

**Salva il token** dalla risposta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  ...
}
```

## Step 2: Upload Immagine

Sostituisci `<TOKEN>` con il token ottenuto sopra:

```bash
curl -X POST http://localhost:8080/api/media ^
  -H "Authorization: Bearer <TOKEN>" ^
  -F "file=@C:\path\to\your\image.jpg"
```

**Salva il Media ID** dalla risposta:
```json
{
  "success": true,
  "media": {
    "id": 1,  ← SALVA QUESTO
    "filename": "...",
    "fileUrl": "http://localhost:8080/api/media/...",
    ...
  }
}
```

## Step 3: Verifica che il Media Esista

Sostituisci `<MEDIA_ID>` con l'ID ricevuto:

```bash
curl http://localhost:8080/api/media/info/<MEDIA_ID>
```

Dovresti ricevere i dati del media. Se ricevi 404, il media non esiste.

## Step 4: Crea Content con Immagine

Sostituisci `<TOKEN>` e `<MEDIA_ID>`:

```bash
curl -X POST http://localhost:8080/api/contents ^
  -H "Authorization: Bearer <TOKEN>" ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Test Con Immagine\",\"slug\":\"test-con-immagine\",\"body\":\"Test body\",\"excerpt\":\"Test excerpt\",\"featuredImageId\":<MEDIA_ID>,\"status\":\"PUBLISHED\"}"
```

Dovresti ricevere il content creato.

## Step 5: Recupera il Content e Verifica l'Immagine

```bash
curl http://localhost:8080/api/contents/test-con-immagine
```

**Verifica la risposta:**

✅ **SUCCESSO** se vedi:
```json
{
  "id": 1,
  "title": "Test Con Immagine",
  "slug": "test-con-immagine",
  "featuredImage": {
    "id": 1,
    "filename": "550e8400-...",
    "originalFilename": "image.jpg",
    "fileUrl": "http://localhost:8080/api/media/550e8400-...",
    "mimeType": "image/jpeg",
    ...
  },
  ...
}
```

❌ **PROBLEMA** se vedi:
```json
{
  ...
  "featuredImage": null,
  ...
}
```

## Step 6: Verifica nei Log

Se `featuredImage` è null, controlla i log dell'applicazione Spring Boot.

Dovresti vedere linee come:
```
INFO  c.e.cms.service.ContentService - Content found - ID: 1, Title: Test Con Immagine
INFO  c.e.cms.service.ContentService - FeaturedImage is null: false
INFO  c.e.cms.service.ContentService - FeaturedImage loaded - ID: 1, Filename: 550e8400-...
```

Se vedi `FeaturedImage is null: true`, il problema è nel caricamento dalla query.

## Step 7: Verifica nel Database

Connettiti al database e esegui:

```sql
SELECT
    c.id,
    c.slug,
    c.featured_image_id,
    m.filename
FROM contents c
LEFT JOIN media m ON c.featured_image_id = m.id
WHERE c.slug = 'test-con-immagine';
```

**Risultato atteso:**
```
 id | slug                | featured_image_id | filename
----+---------------------+-------------------+---------------------------
  1 | test-con-immagine   | 1                 | 550e8400-...-...jpg
```

**Se `featured_image_id` è NULL:**
- Il problema è nel salvataggio del content
- Verifica che il `featuredImageId` nel JSON sia corretto

**Se `filename` è NULL (ma featured_image_id ha un valore):**
- Il media con quell'ID non esiste
- Ricontrolla lo Step 3

**Se tutto è popolato ma l'API ritorna `featuredImage: null`:**
- Il problema è nel lazy loading
- I log dovrebbero mostrare `FeaturedImage is null: true`

## Troubleshooting

### Problema: "Featured image not found" durante creazione content

**Causa**: Il Media ID che passi non esiste.

**Soluzione**: Verifica che il media esista con Step 3.

### Problema: featuredImage è null nell'API ma il DB ha featured_image_id

**Causa**: Lazy loading non funziona, JOIN FETCH non applicato.

**Soluzione**:
1. Verifica che i log mostrino `FeaturedImage is null: true`
2. Controlla che Hibernate esegua la query con JOIN
3. Nei log SQL dovresti vedere:
   ```sql
   select ... from contents c0_
   left outer join media m1_ on c0_.featured_image_id=m1_.id
   where c0_.slug=?
   ```

### Problema: Upload immagine fallisce

Verifica:
- Il file esiste nel path specificato
- Il file è un'immagine valida (jpg, png, gif, webp)
- Il file è < 10MB
- Il token JWT è valido

## Test Rapido con un Solo Comando

Se hai `jq` installato (per Windows: https://stedolan.github.io/jq/download/):

```bash
# Salva questo in un file test.bat e modificalo con le tue credenziali
@echo off
set API_URL=http://localhost:8080
set USERNAME=testuser
set PASSWORD=password123
set IMAGE=C:\path\to\image.jpg

echo Login...
for /f "tokens=*" %%a in ('curl -s -X POST "%API_URL%/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"%USERNAME%\",\"password\":\"%PASSWORD%\"}" ^| jq -r .token') do set TOKEN=%%a

echo Token: %TOKEN:~0,20%...
echo.

echo Upload immagine...
for /f "tokens=*" %%a in ('curl -s -X POST "%API_URL%/api/media" -H "Authorization: Bearer %TOKEN%" -F "file=@%IMAGE%" ^| jq -r .media.id') do set MEDIA_ID=%%a

echo Media ID: %MEDIA_ID%
echo.

echo Crea content...
curl -s -X POST "%API_URL%/api/contents" -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"title\":\"Test\",\"slug\":\"test-media\",\"body\":\"test\",\"featuredImageId\":%MEDIA_ID%,\"status\":\"PUBLISHED\"}"
echo.

echo Recupera content...
curl -s "%API_URL%/api/contents/test-media" | jq .featuredImage

echo.
echo Done!
```
