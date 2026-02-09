# Content Access Control - getContentBySlug

## 🔒 Logica di Controllo Accesso

Il metodo `GET /api/contents/slug/{slug}` ora implementa un controllo di accesso basato sullo **status** del content e sul **ruolo dell'utente**.

---

## 📋 Regole di Accesso

### ✅ Content PUBLISHED
- **Accessibile a TUTTI** (anche senza autenticazione)
- Il view count viene incrementato
- Uso: articoli pubblici del blog, pagine pubbliche

### 🔒 Content DRAFT
- **Accessibile SOLO a**:
  - L'autore del content
  - Utenti con ruolo ADMIN
- **Inaccessibile** a tutti gli altri (ritorna 400 "Content not found")
- Il view count NON viene incrementato
- Uso: bozze in preparazione, articoli non ancora pronti

### 🔒 Content ARCHIVED
- **Accessibile SOLO a**:
  - L'autore del content
  - Utenti con ruolo ADMIN
- **Inaccessibile** a tutti gli altri (ritorna 400 "Content not found")
- Il view count NON viene incrementato
- Uso: articoli vecchi, contenuti rimossi dalla pubblicazione

---

## 🧪 Scenari di Test

### Scenario 1: Content Pubblicato - Accesso Pubblico

**Setup**:
- Content con slug "articolo-pubblico"
- Status: PUBLISHED
- Author: user ID 1

**Test senza autenticazione**:
```bash
curl http://localhost:8080/api/contents/slug/articolo-pubblico
```

**Risultato**: ✅ **SUCCESS** - Content ritornato

---

### Scenario 2: Content Pubblicato - Accesso Autenticato

**Setup**:
- Content con slug "articolo-pubblico"
- Status: PUBLISHED
- Author: user ID 1

**Test con autenticazione (qualsiasi utente)**:
```bash
curl http://localhost:8080/api/contents/slug/articolo-pubblico \
  -H "Authorization: Bearer <TOKEN_QUALSIASI_UTENTE>"
```

**Risultato**: ✅ **SUCCESS** - Content ritornato

---

### Scenario 3: Content Draft - Accesso da Autore

**Setup**:
- Content con slug "bozza-privata"
- Status: DRAFT
- Author: user ID 5 (username: "mario")

**Test con autenticazione dell'autore**:
```bash
# Login come mario (autore)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mario","password":"password123"}' \
  | jq -r '.token')

# Accedi alla bozza
curl http://localhost:8080/api/contents/slug/bozza-privata \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato**: ✅ **SUCCESS** - Content ritornato (sei l'autore)

---

### Scenario 4: Content Draft - Accesso da Altro Utente

**Setup**:
- Content con slug "bozza-privata"
- Status: DRAFT
- Author: user ID 5 (username: "mario")

**Test con autenticazione di altro utente**:
```bash
# Login come luigi (NON autore, NON admin)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"luigi","password":"password123"}' \
  | jq -r '.token')

# Tentativo di accesso alla bozza
curl http://localhost:8080/api/contents/slug/bozza-privata \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato**: ❌ **FAIL** - 400 Bad Request
```json
{
  "message": "Content not found"
}
```

---

### Scenario 5: Content Draft - Accesso da Admin

**Setup**:
- Content con slug "bozza-privata"
- Status: DRAFT
- Author: user ID 5 (username: "mario")

**Test con autenticazione di admin**:
```bash
# Login come admin
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"adminpass"}' \
  | jq -r '.token')

# Accedi alla bozza
curl http://localhost:8080/api/contents/slug/bozza-privata \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato**: ✅ **SUCCESS** - Content ritornato (sei admin)

---

### Scenario 6: Content Draft - Accesso Pubblico (no auth)

**Setup**:
- Content con slug "bozza-privata"
- Status: DRAFT
- Author: user ID 5

**Test senza autenticazione**:
```bash
curl http://localhost:8080/api/contents/slug/bozza-privata
```

**Risultato**: ❌ **FAIL** - 400 Bad Request
```json
{
  "message": "Content not found"
}
```

**Nota**: Per motivi di sicurezza, l'errore è generico "Content not found" invece di "Access denied" per non rivelare l'esistenza di content privati.

---

### Scenario 7: Content Archived - Accesso da Autore

**Setup**:
- Content con slug "articolo-archiviato"
- Status: ARCHIVED
- Author: user ID 5 (username: "mario")

**Test con autenticazione dell'autore**:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mario","password":"password123"}' \
  | jq -r '.token')

curl http://localhost:8080/api/contents/slug/articolo-archiviato \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato**: ✅ **SUCCESS** - Content ritornato (sei l'autore)

---

## 📊 Tabella Riassuntiva

| Status | Utente Non Autenticato | Autore | Admin | Altro Utente |
|--------|------------------------|--------|-------|--------------|
| PUBLISHED | ✅ Sì | ✅ Sì | ✅ Sì | ✅ Sì |
| DRAFT | ❌ No | ✅ Sì | ✅ Sì | ❌ No |
| ARCHIVED | ❌ No | ✅ Sì | ✅ Sì | ❌ No |

---

## 🔍 Dettagli Implementazione

### ContentService.kt - getContentBySlug

```kotlin
fun getContentBySlug(slug: String, username: String?): ContentResponse {
    val content = contentRepository.findBySlug(slug)
        .orElseThrow { IllegalArgumentException("Content not found") }

    // Controllo di accesso per content non pubblicati
    if (content.status != ContentStatus.PUBLISHED) {
        if (username == null) {
            throw IllegalArgumentException("Content not found")
        }

        val currentUser = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("Content not found") }

        val isAuthor = content.author?.id == currentUser.id
        val isAdmin = currentUser.role == UserRole.ADMIN

        if (!isAuthor && !isAdmin) {
            throw IllegalArgumentException("Content not found")
        }
    }

    // View count incrementato SOLO per PUBLISHED
    if (content.status == ContentStatus.PUBLISHED) {
        contentRepository.save(content.copy(viewCount = content.viewCount + 1))
    }

    return content.toResponse()
}
```

### ContentController.kt - getContentBySlug

```kotlin
@GetMapping("/slug/{slug}")
fun getContentBySlug(
    @PathVariable slug: String,
    authentication: Authentication?  // Nullable per supportare accesso pubblico
): ResponseEntity<ContentResponse> {
    val username = authentication?.name
    return ResponseEntity.ok(contentService.getContentBySlug(slug, username))
}
```

---

## 🛡️ Sicurezza

### Protezione da Information Disclosure

L'implementazione restituisce **sempre** lo stesso errore generico "Content not found" per:
- Content inesistente
- Content privato senza autorizzazione

Questo previene che utenti malintenzionati possano:
- Scoprire l'esistenza di content privati
- Enumerare gli slug dei content draft

### Logging

I tentativi di accesso non autorizzato vengono loggati:
```
WARN  c.e.cms.service.ContentService - User luigi attempted to access unpublished content: bozza-privata
```

Gli accessi autorizzati a content non pubblicati vengono loggati:
```
INFO  c.e.cms.service.ContentService - User mario accessing unpublished content: bozza-privata (status: DRAFT)
```

---

## 💡 Casi d'Uso

### Frontend Pubblico (Blog)
```javascript
// Nessuna autenticazione necessaria per articoli pubblicati
async function getArticle(slug) {
  const response = await fetch(`http://localhost:8080/api/contents/slug/${slug}`);
  if (response.ok) {
    return await response.json();
  }
  // Articolo non trovato o non accessibile
  throw new Error('Article not found');
}
```

### Dashboard Autore (Anteprima Bozza)
```javascript
// Con autenticazione per vedere le proprie bozze
async function previewDraft(slug, token) {
  const response = await fetch(`http://localhost:8080/api/contents/slug/${slug}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  if (response.ok) {
    return await response.json();
  }

  // Bozza non accessibile
  throw new Error('Draft not accessible');
}
```

### Admin Dashboard (Moderazione)
```javascript
// Admin può vedere tutti i content indipendentemente dallo status
async function moderateContent(slug, adminToken) {
  const response = await fetch(`http://localhost:8080/api/contents/slug/${slug}`, {
    headers: {
      'Authorization': `Bearer ${adminToken}`
    }
  });

  return await response.json();
}
```

---

## ⚠️ Note Importanti

1. **View Count**: Viene incrementato SOLO per content con status PUBLISHED
2. **Autenticazione Opzionale**: L'endpoint accetta sia richieste autenticate che non autenticate
3. **Errori Generici**: Per sicurezza, gli errori sono sempre "Content not found" indipendentemente dalla causa
4. **Ruolo ADMIN**: Ha accesso a TUTTI i content indipendentemente dallo status
5. **SecurityConfig**: L'endpoint rimane pubblico (GET /api/contents/** permitAll) per permettere accesso ai content pubblicati

---

## 🔄 Workflow Tipico

### Creazione e Pubblicazione di un Articolo

1. **Autore crea bozza**:
   ```bash
   POST /api/contents
   Body: { ..., "status": "DRAFT" }
   ```
   → Content creato con status DRAFT

2. **Autore visualizza anteprima**:
   ```bash
   GET /api/contents/slug/mia-bozza
   Header: Authorization: Bearer <token-autore>
   ```
   → ✅ Successo (è l'autore)

3. **Altri utenti NON possono vedere**:
   ```bash
   GET /api/contents/slug/mia-bozza
   ```
   → ❌ "Content not found"

4. **Admin può revisionare**:
   ```bash
   GET /api/contents/slug/mia-bozza
   Header: Authorization: Bearer <token-admin>
   ```
   → ✅ Successo (è admin)

5. **Autore pubblica**:
   ```bash
   PUT /api/contents/{id}
   Body: { ..., "status": "PUBLISHED" }
   ```
   → Content ora pubblico

6. **Tutti possono vedere**:
   ```bash
   GET /api/contents/slug/mia-bozza
   ```
   → ✅ Successo (è pubblicato)
