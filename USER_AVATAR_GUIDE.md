# Guida Gestione Avatar Utente

## 📸 Funzionalità Implementata

Ogni utente può caricare un'immagine avatar che viene salvata sul filesystem e il percorso memorizzato nel database nella tabella `users` campo `avatar_path`.

---

## 🗄️ Schema Database

### Tabella users - Campo aggiunto

```sql
ALTER TABLE users ADD COLUMN avatar_path VARCHAR(500);
```

**Struttura**:
```
users
├── id
├── username
├── email
├── password_hash
├── first_name
├── last_name
├── role
├── avatar_path  ← NUOVO CAMPO
├── is_active
├── created_at
└── updated_at
```

---

## 📁 Struttura File System

Gli avatar vengono salvati in:
```
D:/projects/cms/cms-backend/avatars/
  ├── 1_550e8400-e29b-41d4-a716-446655440000.jpg
  ├── 2_a72c9b11-5f32-4d89-b8e2-1a3c4d5e6f7a.png
  └── 3_f4d8e2a3-9c1b-4e5f-a7d8-2b3c4d5e6f7a.webp
```

**Naming convention**: `{userId}_{UUID}.{extension}`

---

## 🔐 Endpoint API

### 1. Upload Avatar (Autenticato)

**POST** `/api/users/avatar`

**Autenticazione**: Richiesta (JWT token)

**Body**: `multipart/form-data`
- `file`: file immagine (jpg, jpeg, png, gif, webp)

**Limiti**:
- Max dimensione: **5MB**
- Formati: jpg, jpeg, png, gif, webp

**Esempio Request**:
```bash
curl -X POST http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@/path/to/avatar.jpg"
```

**Esempio Response** (Success):
```json
{
  "success": "true",
  "message": "Avatar uploaded successfully",
  "avatarUrl": "http://localhost:8080/api/users/avatars/1_550e8400-e29b-41d4-a716-446655440000.jpg"
}
```

**Esempio Response** (Error):
```json
{
  "success": "false",
  "message": "Invalid file type. Allowed types: jpg, jpeg, png, gif, webp"
}
```

---

### 2. Get Avatar URL (Autenticato)

**GET** `/api/users/avatar`

**Autenticazione**: Richiesta

**Esempio Request**:
```bash
curl http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer <TOKEN>"
```

**Esempio Response**:
```json
{
  "avatarUrl": "http://localhost:8080/api/users/avatars/1_550e8400-e29b-41d4-a716-446655440000.jpg"
}
```

Se l'utente non ha avatar:
```json
{
  "avatarUrl": null
}
```

---

### 3. Serve Avatar (Pubblico)

**GET** `/api/users/avatars/{filename}`

**Autenticazione**: NON richiesta (pubblico)

**Esempio Request**:
```bash
curl http://localhost:8080/api/users/avatars/1_550e8400-e29b-41d4-a716-446655440000.jpg
```

**Response**: Immagine binaria con Content-Type appropriato

**Uso in HTML**:
```html
<img src="http://localhost:8080/api/users/avatars/1_550e8400-e29b-41d4-a716-446655440000.jpg"
     alt="User Avatar" />
```

---

### 4. Delete Avatar (Autenticato)

**DELETE** `/api/users/avatar`

**Autenticazione**: Richiesta

**Esempio Request**:
```bash
curl -X DELETE http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer <TOKEN>"
```

**Esempio Response**:
```json
{
  "success": "true",
  "message": "Avatar deleted successfully"
}
```

**Errore** (se non hai avatar):
```json
{
  "success": "false",
  "message": "User has no avatar"
}
```

---

## 🔄 Workflow Completo

### Scenario 1: Primo Upload Avatar

**Step 1**: Login
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mario","password":"password123"}' \
  | jq -r '.token')
```

**Step 2**: Upload avatar
```bash
AVATAR_URL=$(curl -s -X POST http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@avatar.jpg" \
  | jq -r '.avatarUrl')

echo "Avatar URL: $AVATAR_URL"
```

**Step 3**: L'avatar è ora visibile
```bash
# Nel browser o curl
curl $AVATAR_URL --output downloaded-avatar.jpg
```

---

### Scenario 2: Aggiornamento Avatar

**Comportamento**: Upload di un nuovo avatar **sostituisce** quello vecchio.

```bash
# Upload nuovo avatar
curl -X POST http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@new-avatar.png"
```

**Cosa succede**:
1. Il vecchio file viene **eliminato** dal filesystem
2. Il nuovo file viene salvato
3. Il campo `avatar_path` viene aggiornato nel database

---

### Scenario 3: Eliminazione Avatar

```bash
# Elimina avatar
curl -X DELETE http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN"
```

**Cosa succede**:
1. Il file viene eliminato dal filesystem
2. Il campo `avatar_path` viene impostato a `null` nel database

---

## 📊 Login con Avatar

La risposta di login **include automaticamente** l'URL dell'avatar se presente.

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mario","password":"password123"}'
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "id": 1,
  "username": "mario",
  "email": "mario@example.com",
  "role": "AUTHOR",
  "avatarUrl": "http://localhost:8080/api/users/avatars/1_550e8400-e29b-41d4-a716-446655440000.jpg"
}
```

Se l'utente non ha avatar:
```json
{
  ...
  "avatarUrl": null
}
```

---

## 💻 Esempi Frontend

### React - Upload Avatar

```javascript
async function uploadAvatar(file, token) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('http://localhost:8080/api/users/avatar', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  const data = await response.json();

  if (data.success === "true") {
    console.log('Avatar URL:', data.avatarUrl);
    return data.avatarUrl;
  } else {
    throw new Error(data.message);
  }
}

// Uso
const fileInput = document.getElementById('avatarInput');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];
  const token = sessionStorage.getItem('jwt_token');

  try {
    const avatarUrl = await uploadAvatar(file, token);
    // Aggiorna UI con nuovo avatar
    document.getElementById('userAvatar').src = avatarUrl;
  } catch (error) {
    console.error('Upload failed:', error.message);
  }
});
```

---

### React Component - Avatar Display

```javascript
function UserAvatar({ user }) {
  const [avatarUrl, setAvatarUrl] = useState(user.avatarUrl);

  const handleAvatarUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const token = sessionStorage.getItem('jwt_token');

    try {
      const response = await fetch('http://localhost:8080/api/users/avatar', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
      });

      const data = await response.json();

      if (data.success === "true") {
        setAvatarUrl(data.avatarUrl);
      }
    } catch (error) {
      console.error('Upload error:', error);
    }
  };

  const handleAvatarDelete = async () => {
    const token = sessionStorage.getItem('jwt_token');

    try {
      const response = await fetch('http://localhost:8080/api/users/avatar', {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      const data = await response.json();

      if (data.success === "true") {
        setAvatarUrl(null);
      }
    } catch (error) {
      console.error('Delete error:', error);
    }
  };

  return (
    <div className="avatar-container">
      {avatarUrl ? (
        <img src={avatarUrl} alt="User Avatar" className="avatar" />
      ) : (
        <div className="avatar-placeholder">No Avatar</div>
      )}

      <input
        type="file"
        accept="image/*"
        onChange={handleAvatarUpload}
        id="avatarUpload"
      />
      <label htmlFor="avatarUpload">Upload Avatar</label>

      {avatarUrl && (
        <button onClick={handleAvatarDelete}>Delete Avatar</button>
      )}
    </div>
  );
}
```

---

### Vanilla JS - Avatar al Login

```javascript
async function login(username, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  const data = await response.json();

  if (response.ok) {
    // Salva in sessionStorage
    sessionStorage.setItem('jwt_token', data.token);
    sessionStorage.setItem('user', JSON.stringify({
      id: data.id,
      username: data.username,
      email: data.email,
      role: data.role,
      avatarUrl: data.avatarUrl  // ← Avatar URL dal login
    }));

    // Mostra avatar se presente
    if (data.avatarUrl) {
      document.getElementById('userAvatar').src = data.avatarUrl;
    }
  }
}
```

---

## 🛡️ Sicurezza

### Permessi

| Endpoint | Autenticazione | Chi può accedere |
|----------|----------------|------------------|
| `POST /api/users/avatar` | ✅ Richiesta | Solo l'utente stesso |
| `GET /api/users/avatar` | ✅ Richiesta | Solo l'utente stesso |
| `DELETE /api/users/avatar` | ✅ Richiesta | Solo l'utente stesso |
| `GET /api/users/avatars/{filename}` | ❌ Pubblico | Tutti |

### Validazioni Implementate

1. **Tipo file**: Solo immagini (jpg, jpeg, png, gif, webp)
2. **Dimensione**: Max 5MB
3. **Ownership**: Un utente può modificare solo il proprio avatar
4. **Path traversal**: Protetto con `normalize()`
5. **Sovrascrittura**: Upload nuovo avatar elimina il vecchio automaticamente

---

## 📝 Note Implementative

### Gestione Storage

- **Directory**: `D:/projects/cms/cms-backend/avatars/`
- Creata automaticamente al primo avvio se non esiste
- Ogni utente ha **un solo** avatar alla volta
- Il vecchio avatar viene **eliminato** quando ne carichi uno nuovo

### Naming File

Formato: `{userId}_{UUID}.{extension}`

**Esempio**: `1_550e8400-e29b-41d4-a716-446655440000.jpg`

**Vantaggi**:
- `userId` prefix: Facile identificare a chi appartiene
- `UUID`: Garantisce unicità
- `extension`: Mantiene il tipo di file

### Database

Campo `avatar_path` nella tabella `users`:
- **Type**: VARCHAR(500)
- **Nullable**: Sì (non tutti gli utenti hanno avatar)
- **Esempio valore**: `D:/projects/cms/cms-backend/avatars/1_550e8400-....jpg`

---

## ⚠️ Troubleshooting

### Errore: "Could not create avatar directory"

**Causa**: Permessi insufficienti per creare la directory.

**Soluzione**:
1. Crea manualmente la directory: `D:/projects/cms/cms-backend/avatars`
2. Assicurati che l'applicazione abbia permessi di scrittura

---

### Errore: "File size exceeds maximum limit of 5MB"

**Causa**: Immagine troppo grande.

**Soluzione**: Comprimi l'immagine prima dell'upload.

**Aumentare il limite** (in application.yml):
```yaml
avatar:
  upload:
    max-size: 10485760  # 10MB in bytes
```

E in UserService.kt:
```kotlin
val maxSize = 10 * 1024 * 1024L // 10MB
```

---

### Avatar non si vede dopo upload

**Debug**:
1. Verifica che il file sia stato salvato:
   ```bash
   ls D:/projects/cms/cms-backend/avatars/
   ```

2. Verifica il database:
   ```sql
   SELECT id, username, avatar_path FROM users WHERE id = 1;
   ```

3. Verifica l'URL generato:
   ```bash
   curl http://localhost:8080/api/users/avatar \
     -H "Authorization: Bearer <TOKEN>"
   ```

4. Prova a scaricare direttamente:
   ```bash
   curl http://localhost:8080/api/users/avatars/{filename} --output test.jpg
   ```

---

## 🚀 Test Completo

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mario","password":"password123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# 2. Verifica avatar iniziale (dovrebbe essere null)
curl -s http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  | jq

# 3. Upload avatar
curl -s -X POST http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@avatar.jpg" \
  | jq

# 4. Verifica nuovo avatar
AVATAR_URL=$(curl -s http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  | jq -r '.avatarUrl')

echo "Avatar URL: $AVATAR_URL"

# 5. Scarica avatar
curl "$AVATAR_URL" --output downloaded-avatar.jpg

# 6. Elimina avatar
curl -s -X DELETE http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  | jq

# 7. Verifica eliminazione
curl -s http://localhost:8080/api/users/avatar \
  -H "Authorization: Bearer $TOKEN" \
  | jq
```

---

## 🔄 Differenze con Media (Content Images)

| Feature | Avatar (users) | Media (content images) |
|---------|----------------|------------------------|
| **Tabella** | users.avatar_path | media (tabella separata) |
| **Directory** | `/avatars` | `/uploads` |
| **Max size** | 5MB | 10MB |
| **Quantità** | 1 per utente | Illimitate |
| **Ownership** | Solo l'utente | Autore + Admin |
| **Sovrascrittura** | Automatica | No (file separati) |
| **Metadati** | Solo path | filename, mimeType, fileSize, etc. |
| **Accesso pubblico** | Sì (GET) | Sì (GET) |

---

## ✅ Checklist Implementazione Frontend

- [ ] Form per upload avatar con input file
- [ ] Preview dell'avatar corrente
- [ ] Gestione stato avatar (null vs URL)
- [ ] Bottone per eliminare avatar
- [ ] Mostrare avatar in navbar/profilo
- [ ] Salvare avatarUrl dal login in sessionStorage
- [ ] Validazione client-side (tipo file, dimensione)
- [ ] Gestione errori upload
- [ ] Loading state durante upload
- [ ] Avatar placeholder per utenti senza avatar
