# Guida Multi-Sessione - Gestione di Sessioni Utente Multiple

## 🎯 Obiettivo

Permettere di aprire più tab Chrome con utenti diversi loggati contemporaneamente nello stesso browser.

---

## 🔑 Come Funziona

### Backend (già implementato ✅)

Il backend JWT è **stateless** e supporta nativamente sessioni multiple:
- Ogni login genera un **token JWT univoco** con un **sessionId** incorporato
- Ogni token è indipendente e può essere validato separatamente
- Non c'è stato condiviso lato server - ogni richiesta è autenticata dal suo token

**Modifiche implementate**:
1. Ogni token JWT contiene un claim `sessionId` (UUID univoco)
2. La risposta di login include `sessionId` nell'oggetto `AuthResponse`

**Esempio risposta login**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "id": 1,
  "username": "mario",
  "email": "mario@example.com",
  "role": "AUTHOR"
}
```

---

## 💻 Implementazione Frontend

### Problema con localStorage

**❌ NON FUNZIONA** - localStorage è condiviso tra tutte le tab:

```javascript
// ERRATO - tutte le tab vedono lo stesso token
localStorage.setItem('jwt_token', token);
```

Se fai login in una tab, **tutte le altre tab** vedono lo stesso token e quindi lo stesso utente.

---

### ✅ Soluzione: sessionStorage

**sessionStorage** è isolato per ogni tab - ogni tab ha il suo storage indipendente.

#### 1. Salva token in sessionStorage (non localStorage)

```javascript
async function login(username, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  const data = await response.json();

  if (response.ok) {
    // ✅ USA sessionStorage invece di localStorage
    sessionStorage.setItem('jwt_token', data.token);
    sessionStorage.setItem('session_id', data.sessionId);
    sessionStorage.setItem('user', JSON.stringify({
      id: data.id,
      username: data.username,
      email: data.email,
      role: data.role
    }));

    console.log('Logged in as:', data.username);
    console.log('Session ID:', data.sessionId);
  }
}
```

#### 2. Leggi token da sessionStorage

```javascript
function getAuthToken() {
  // ✅ Leggi da sessionStorage
  return sessionStorage.getItem('jwt_token');
}

function getCurrentUser() {
  const userJson = sessionStorage.getItem('user');
  return userJson ? JSON.parse(userJson) : null;
}

function getSessionId() {
  return sessionStorage.getItem('session_id');
}
```

#### 3. Usa token nelle richieste API

```javascript
async function fetchProtectedData() {
  const token = getAuthToken();

  if (!token) {
    console.error('No token found - user not logged in');
    return;
  }

  const response = await fetch('http://localhost:8080/api/contents/my-contents', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  return await response.json();
}
```

#### 4. Logout (pulisci sessionStorage)

```javascript
function logout() {
  // Pulisci solo il session storage della tab corrente
  sessionStorage.removeItem('jwt_token');
  sessionStorage.removeItem('session_id');
  sessionStorage.removeItem('user');

  console.log('Logged out');
  window.location.href = '/login';
}
```

---

## 🧪 Test Multi-Sessione

### Scenario: 3 Utenti in 3 Tab Diverse

**Tab 1 - mario**:
1. Apri `http://localhost:3000`
2. Login come `mario / password123`
3. sessionStorage salvato:
   ```
   jwt_token: "eyJhbGciOi...mario..."
   session_id: "550e8400-..."
   user: {"username":"mario",...}
   ```

**Tab 2 - luigi** (NUOVA TAB):
1. Apri **nuova tab** `http://localhost:3000`
2. Login come `luigi / password456`
3. sessionStorage salvato (indipendente da Tab 1):
   ```
   jwt_token: "eyJhbGciOi...luigi..."
   session_id: "a72c9b11-..."
   user: {"username":"luigi",...}
   ```

**Tab 3 - admin** (NUOVA TAB):
1. Apri **nuova tab** `http://localhost:3000`
2. Login come `admin / adminpass`
3. sessionStorage salvato (indipendente da Tab 1 e 2):
   ```
   jwt_token: "eyJhbGciOi...admin..."
   session_id: "f4d8e2a3-..."
   user: {"username":"admin",...}
   ```

**Risultato**:
- Tab 1 vede i content di mario
- Tab 2 vede i content di luigi
- Tab 3 vede tutti i content (admin)

✅ **Ogni tab è completamente indipendente!**

---

## 📝 Esempio Completo React/Vue/Vanilla JS

### React con Context

```javascript
// AuthContext.js
import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);

  useEffect(() => {
    // Carica da sessionStorage al mount
    const savedToken = sessionStorage.getItem('jwt_token');
    const savedUser = sessionStorage.getItem('user');

    if (savedToken && savedUser) {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
    }
  }, []);

  const login = async (username, password) => {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();

    if (response.ok) {
      sessionStorage.setItem('jwt_token', data.token);
      sessionStorage.setItem('session_id', data.sessionId);
      sessionStorage.setItem('user', JSON.stringify({
        id: data.id,
        username: data.username,
        email: data.email,
        role: data.role
      }));

      setToken(data.token);
      setUser({
        id: data.id,
        username: data.username,
        email: data.email,
        role: data.role
      });
    }
  };

  const logout = () => {
    sessionStorage.clear();
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
```

### Axios Interceptor

```javascript
// api.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

// Interceptor per aggiungere token automaticamente
api.interceptors.request.use(config => {
  const token = sessionStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;

// Uso
import api from './api';

async function getMyContents() {
  const response = await api.get('/contents/my-contents');
  return response.data;
}
```

---

## 🔄 Differenze localStorage vs sessionStorage

| Feature | localStorage | sessionStorage |
|---------|-------------|----------------|
| **Scope** | Globale (tutte le tab) | Per tab (isolato) |
| **Persistenza** | Permanente (finché non eliminato) | Durata della tab/sessione |
| **Multi-sessione** | ❌ No (condiviso) | ✅ Sì (isolato) |
| **Uso ideale** | Preferenze UI, tema | Token di autenticazione |

---

## 🚨 Attenzioni e Limiti

### ⚠️ Duplicazione tab con Ctrl+T

Se duplichi una tab con `Ctrl+Shift+T` (ripristina tab chiusa) o duplici una tab esistente:
- Il **sessionStorage viene copiato** nella nuova tab
- Le due tab avranno lo stesso token inizialmente
- Ma sono comunque indipendenti: se fai logout in una, l'altra rimane loggata

**Soluzione**: Dopo il login, salva un `tabId` univoco:
```javascript
function login(username, password) {
  // ... login normale ...

  // Genera ID univoco per questa tab
  const tabId = Date.now() + '-' + Math.random();
  sessionStorage.setItem('tab_id', tabId);
}

// Controlla se è una tab duplicata
window.addEventListener('load', () => {
  const existingTabId = sessionStorage.getItem('tab_id');
  if (existingTabId) {
    // Tab duplicata - potenzialmente chiedi rilogin
    console.warn('Tab duplicated - consider re-authentication');
  }
});
```

### ⚠️ Refresh della pagina

- sessionStorage **persiste** al refresh (F5)
- L'utente rimane loggato dopo il reload
- Questo è il comportamento desiderato

### ⚠️ Token scaduti

Il token JWT ha una scadenza (default 24 ore). Quando scade:
- Il backend ritornerà 401 Unauthorized
- Il frontend deve intercettare e fare logout automatico

```javascript
// Interceptor per gestire token scaduti
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      console.error('Token expired or invalid');
      sessionStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 📊 Verifica Multi-Sessione

### Backend - Verifica sessionId nel token

Puoi aggiungere un endpoint per verificare la sessione corrente:

```bash
# Decodifica il token JWT per vedere il sessionId
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

Il backend potrebbe ritornare:
```json
{
  "username": "mario",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "issuedAt": "2026-01-19T10:30:00",
  "expiresAt": "2026-01-20T10:30:00"
}
```

### Frontend - Verifica tab isolate

Console del browser:
```javascript
// In Tab 1
console.log('Token:', sessionStorage.getItem('jwt_token'));
// Output: "eyJhbGci...mario..."

// In Tab 2 (diversa)
console.log('Token:', sessionStorage.getItem('jwt_token'));
// Output: "eyJhbGci...luigi..." (DIVERSO!)
```

---

## 🎯 Checklist Implementazione

Frontend deve:
- [ ] Usare **sessionStorage** invece di localStorage per token
- [ ] Salvare `jwt_token`, `session_id`, `user` in sessionStorage al login
- [ ] Leggere da sessionStorage nelle richieste API
- [ ] Pulire sessionStorage al logout
- [ ] Gestire errori 401 (token scaduto/invalido)
- [ ] Testare aprendo multiple tab con utenti diversi

Backend (già fatto ✅):
- [x] Generare sessionId univoco nel token JWT
- [x] Includere sessionId nella risposta di login
- [x] Validare token indipendentemente per ogni richiesta

---

## 🔍 Debug

### Visualizza sessionStorage nel browser

1. Apri DevTools (F12)
2. Tab **Application** (Chrome) / **Storage** (Firefox)
3. Sidebar: **Session Storage** → `http://localhost:3000`
4. Vedi tutte le chiavi salvate

### Test rapido

```javascript
// Console del browser in Tab 1
sessionStorage.setItem('test', 'tab1');
console.log(sessionStorage.getItem('test')); // "tab1"

// Console del browser in Tab 2
console.log(sessionStorage.getItem('test')); // null (non esiste!)
sessionStorage.setItem('test', 'tab2');
console.log(sessionStorage.getItem('test')); // "tab2"

// Torna in Tab 1
console.log(sessionStorage.getItem('test')); // ancora "tab1"
```

Ogni tab ha il suo storage completamente isolato! ✅

---

## 📚 Risorse

- [MDN sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage)
- [MDN localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)
- [JWT.io - Debug tokens](https://jwt.io/)
