# Riepilogo Controllo Accesso - Tutti gli Endpoint Content

## ✅ Endpoint con Controllo Accesso Implementato

Tutti i metodi di listing dei content ora implementano il **controllo di accesso basato sull'utente** con la seguente logica:

### 🔒 Regola Generale
- **Content PUBLISHED**: Visibili a TUTTI (anche senza autenticazione)
- **Content DRAFT/ARCHIVED**: Visibili SOLO a:
  - L'autore del content
  - Utenti con ruolo ADMIN

---

## 📋 Elenco Endpoint Protetti

### 1. GET /api/contents
**Tutti i content**

```bash
# Senza autenticazione: solo PUBLISHED
curl http://localhost:8080/api/contents

# Con autenticazione: PUBLISHED + i propri DRAFT/ARCHIVED
curl http://localhost:8080/api/contents \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 2. GET /api/contents/category/{categoryId}
**Content per categoria**

```bash
# Senza autenticazione: solo PUBLISHED della categoria
curl http://localhost:8080/api/contents/category/1

# Con autenticazione: PUBLISHED + i propri DRAFT/ARCHIVED della categoria
curl http://localhost:8080/api/contents/category/1 \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 3. GET /api/contents/tag/{tagId}
**Content per tag**

```bash
# Senza autenticazione: solo PUBLISHED con quel tag
curl http://localhost:8080/api/contents/tag/5

# Con autenticazione: PUBLISHED + i propri DRAFT/ARCHIVED con quel tag
curl http://localhost:8080/api/contents/tag/5 \
  -H "Authorization: Bearer <TOKEN>"
```

---

### 4. GET /api/contents/author/{authorId}
**Content per autore**

```bash
# Senza autenticazione: solo PUBLISHED dell'autore
curl http://localhost:8080/api/contents/author/3

# Con autenticazione come autore: TUTTI i propri content
curl http://localhost:8080/api/contents/author/3 \
  -H "Authorization: Bearer <TOKEN_AUTORE>"

# Con autenticazione come admin: TUTTI i content dell'autore
curl http://localhost:8080/api/contents/author/3 \
  -H "Authorization: Bearer <TOKEN_ADMIN>"
```

---

### 5. GET /api/contents/slug/{slug}
**Singolo content per slug**

```bash
# Senza autenticazione: solo se PUBLISHED
curl http://localhost:8080/api/contents/slug/mio-articolo

# Con autenticazione: PUBLISHED + i propri DRAFT/ARCHIVED
curl http://localhost:8080/api/contents/slug/mia-bozza \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📊 Tabella Riepilogativa Accessi

| Endpoint | Utente Non Auth | Autore | Admin | Altro Utente |
|----------|----------------|--------|-------|--------------|
| **GET /api/contents** | Solo PUBLISHED | PUBLISHED + propri DRAFT/ARCHIVED | TUTTI | PUBLISHED + propri DRAFT/ARCHIVED |
| **GET /api/contents/published** | Solo PUBLISHED | Solo PUBLISHED | Solo PUBLISHED | Solo PUBLISHED |
| **GET /api/contents/category/{id}** | Solo PUBLISHED | PUBLISHED + propri DRAFT/ARCHIVED | TUTTI | PUBLISHED + propri DRAFT/ARCHIVED |
| **GET /api/contents/tag/{id}** | Solo PUBLISHED | PUBLISHED + propri DRAFT/ARCHIVED | TUTTI | PUBLISHED + propri DRAFT/ARCHIVED |
| **GET /api/contents/author/{id}** | Solo PUBLISHED | PUBLISHED + propri DRAFT/ARCHIVED | TUTTI | PUBLISHED + propri DRAFT/ARCHIVED |
| **GET /api/contents/slug/{slug}** | Solo PUBLISHED | PUBLISHED + propri DRAFT/ARCHIVED | TUTTI | PUBLISHED + propri DRAFT/ARCHIVED |
| **GET /api/contents/my-contents** | ❌ Richiede auth | Propri TUTTI | Propri TUTTI | Propri TUTTI |

---

## 🔧 Implementazione Tecnica

### Funzione Helper nel ContentService

```kotlin
/**
 * Filtra i content in base ai permessi dell'utente:
 * - PUBLISHED: visibili a tutti
 * - DRAFT/ARCHIVED: visibili solo all'autore o agli admin
 */
private fun filterContentsByPermissions(contents: Page<Content>, username: String?): Page<ContentResponse> {
    // Se l'utente non è autenticato, mostra solo i content PUBLISHED
    if (username == null) {
        return contents
            .filter { it.status == ContentStatus.PUBLISHED }
            .map { it.toResponse() }
    }

    // Ottieni l'utente autenticato
    val currentUser = userRepository.findByUsername(username).orElse(null)
    val isAdmin = currentUser?.role == UserRole.ADMIN

    // Filtra i content in base ai permessi
    return contents
        .filter { content ->
            when (content.status) {
                ContentStatus.PUBLISHED -> true
                ContentStatus.DRAFT, ContentStatus.ARCHIVED -> {
                    // Mostra solo se sei l'autore o admin
                    isAdmin || content.author?.id == currentUser?.id
                }
            }
        }
        .map { it.toResponse() }
}
```

### Pattern nei Controller

Tutti i controller ora accettano `authentication: Authentication?` (nullable):

```kotlin
@GetMapping("/category/{categoryId}")
fun getContentsByCategory(
    @PathVariable categoryId: Long,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
    authentication: Authentication?  // Nullable per supportare accesso pubblico
): ResponseEntity<Page<ContentResponse>> {
    val username = authentication?.name
    return ResponseEntity.ok(
        contentService.getContentsByCategory(categoryId, PageRequest.of(page, size), username)
    )
}
```

---

## 🧪 Scenari di Test

### Scenario 1: Utente Pubblico Visualizza Blog
```bash
# Ottiene solo articoli pubblicati
curl http://localhost:8080/api/contents
curl http://localhost:8080/api/contents/category/1
curl http://localhost:8080/api/contents/tag/5
curl http://localhost:8080/api/contents/author/3
```

**Risultato**: Solo content con status PUBLISHED

---

### Scenario 2: Autore Visualizza Dashboard
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mario","password":"password123"}' \
  | jq -r '.token')

# Visualizza tutti i content (incluse le proprie bozze)
curl http://localhost:8080/api/contents \
  -H "Authorization: Bearer $TOKEN"

# Visualizza i propri content per categoria (incluse bozze)
curl http://localhost:8080/api/contents/category/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato**:
- Content PUBLISHED di tutti
- Content DRAFT/ARCHIVED dell'utente loggato

---

### Scenario 3: Admin Modera Tutti i Content
```bash
# Login come admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"adminpass"}' \
  | jq -r '.token')

# Visualizza TUTTI i content
curl http://localhost:8080/api/contents \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Risultato**: TUTTI i content (PUBLISHED, DRAFT, ARCHIVED) di tutti gli utenti

---

### Scenario 4: Autore Visualizza Profilo di Altro Autore
```bash
# Login come luigi
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"luigi","password":"password123"}' \
  | jq -r '.token')

# Visualizza content di mario (ID 3)
curl http://localhost:8080/api/contents/author/3 \
  -H "Authorization: Bearer $TOKEN"
```

**Risultato**:
- Content PUBLISHED di mario
- Content DRAFT/ARCHIVED di luigi stesso (se ne ha)
- NON mostra le bozze di mario (non sei né autore né admin)

---

## 💡 Casi d'Uso Frontend

### Blog Pubblico
```javascript
// Nessun token necessario - solo articoli pubblicati
async function getBlogPosts() {
  const response = await fetch('http://localhost:8080/api/contents/published');
  return await response.json();
}

async function getPostsByCategory(categoryId) {
  const response = await fetch(`http://localhost:8080/api/contents/category/${categoryId}`);
  return await response.json();
}
```

---

### Dashboard Autore
```javascript
// Con token - vede anche le proprie bozze
async function getMyDashboard(token) {
  const response = await fetch('http://localhost:8080/api/contents', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}

async function previewMyDraft(slug, token) {
  const response = await fetch(`http://localhost:8080/api/contents/slug/${slug}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}
```

---

### Dashboard Admin
```javascript
// Admin vede tutto
async function getAllContentForModeration(adminToken) {
  const response = await fetch('http://localhost:8080/api/contents', {
    headers: {
      'Authorization': `Bearer ${adminToken}`
    }
  });
  return await response.json();
}

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

1. **Paginazione**: Tutti gli endpoint supportano paginazione con `?page=0&size=10`
2. **View Count**: Incrementato SOLO per content PUBLISHED
3. **Autenticazione Opzionale**: Gli endpoint accettano sia richieste autenticate che pubbliche
4. **Endpoint /published**: Rimane invariato, mostra SOLO content PUBLISHED a tutti
5. **SecurityConfig**: Gli endpoint rimangono pubblici (GET permitAll) per permettere accesso ai content pubblicati
6. **Filtro Lato Service**: Il filtro avviene nel service, non nel repository, per mantenere la logica di business separata

---

## 🔐 Sicurezza

- ✅ Content privati (DRAFT/ARCHIVED) non sono visibili pubblicamente
- ✅ Solo autori e admin possono vedere content non pubblicati
- ✅ Gli errori sono generici per evitare information disclosure
- ✅ Logging degli accessi per audit

---

## 📝 Endpoint NON Modificati

Questi endpoint rimangono invariati:

- **GET /api/contents/published** - Sempre solo PUBLISHED
- **GET /api/contents/my-contents** - Sempre solo i propri (già protetto)
- **POST /api/contents** - Creazione (richiede autenticazione)
- **PUT /api/contents/{id}** - Modifica (richiede autenticazione)
- **DELETE /api/contents/{id}** - Eliminazione (richiede autenticazione)
