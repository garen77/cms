# Guida Configurazione Ambienti - CMS Backend

## 📋 Overview

Il progetto supporta configurazioni separate per **ambiente di sviluppo** e **ambiente di produzione** usando:
- **Spring Profiles** (`dev`, `prod`)
- **Variabili d'ambiente** (tramite file `.env` o system environment)

---

## 🗂️ File di Configurazione

| File | Scopo | Committato Git | Descrizione |
|------|-------|----------------|-------------|
| `application.yml` | Base | ✅ Sì | Configurazione base con variabili d'ambiente e default |
| `application-dev.yml` | Sviluppo | ✅ Sì | Valori hardcoded per dev locale |
| `application-prod.yml` | Produzione | ✅ Sì | Richiede variabili d'ambiente obbligatorie |
| `.env.example` | Template | ✅ Sì | Template per creare `.env` |
| `.env` | Locale | ❌ NO | File con valori reali (secret) |

---

## 🛠️ Setup Ambiente di Sviluppo

### Metodo 1: Usa application-dev.yml (Raccomandato)

Il file `application-dev.yml` ha già valori hardcoded per dev locale.

```bash
# Avvia con profilo dev
gradlew bootRun --args='--spring.profiles.active=dev'

# Oppure su Windows
set SPRING_PROFILES_ACTIVE=dev
gradlew.bat bootRun
```

**Valori di default dev**:
- Database: `localhost:5432/cms_db`
- Username: `postgres`
- Password: `Qwertyui123!`
- Upload dir: `./uploads`, `./avatars`
- Server: `http://localhost:8080`

---

### Metodo 2: Usa file .env

Se vuoi personalizzare i valori senza modificare `application-dev.yml`:

**Step 1**: Crea file `.env`
```bash
# Copia template
cp .env.example .env

# Oppure su Windows
copy .env.example .env
```

**Step 2**: Modifica `.env` con i tuoi valori
```bash
# Apri .env e modifica
DB_PASSWORD=mia_password
MEDIA_UPLOAD_DIR=D:/my-custom-path/uploads
```

**Step 3**: Carica variabili e avvia

**Linux/Mac**:
```bash
export $(cat .env | xargs)
gradlew bootRun
```

**Windows** (PowerShell):
```powershell
Get-Content .env | ForEach-Object {
    $name, $value = $_.split('=')
    Set-Item -Path "env:$name" -Value $value
}
gradlew.bat bootRun
```

---

## 🚀 Setup Ambiente di Produzione

### Prerequisiti

1. **Database PostgreSQL** configurato e raggiungibile
2. **Directory upload** create con permessi di scrittura
3. **JWT secret** generato (NON usare quello di dev!)

---

### Step 1: Genera JWT Secret

**Genera un nuovo secret sicuro**:

```bash
# Con Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"

# Con OpenSSL
openssl rand -base64 64

# Con Python
python -c "import os, base64; print(base64.b64encode(os.urandom(64)).decode())"
```

**IMPORTANTE**: NON riutilizzare il secret di sviluppo in produzione!

---

### Step 2: Configura Variabili d'Ambiente

**Su Linux/Ubuntu Server**:

Crea `/etc/systemd/system/cms-backend.service`:

```ini
[Unit]
Description=CMS Backend Service
After=postgresql.service

[Service]
Type=simple
User=cms
WorkingDirectory=/opt/cms-backend

# Variabili d'ambiente
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_URL=jdbc:postgresql://localhost:5432/cms_prod"
Environment="DB_USERNAME=cms_user"
Environment="DB_PASSWORD=secure_password_here"
Environment="DB_DDL_AUTO=validate"
Environment="DB_SHOW_SQL=false"
Environment="MEDIA_UPLOAD_DIR=/var/cms/uploads"
Environment="MEDIA_BASE_URL=https://your-domain.com/api/media"
Environment="AVATAR_UPLOAD_DIR=/var/cms/avatars"
Environment="AVATAR_BASE_URL=https://your-domain.com/api/users/avatars"
Environment="JWT_SECRET=your_generated_secret_here"
Environment="JWT_EXPIRATION=86400000"
Environment="SERVER_PORT=8080"

ExecStart=/usr/bin/java -jar /opt/cms-backend/cms-backend.jar

[Install]
WantedBy=multi-user.target
```

**Oppure con file .env**:

```bash
# Crea .env in /opt/cms-backend/.env
sudo nano /opt/cms-backend/.env

# Inserisci tutte le variabili
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/cms_prod
DB_USERNAME=cms_user
DB_PASSWORD=secure_password
...
```

Poi carica e avvia:
```bash
export $(cat /opt/cms-backend/.env | xargs)
java -jar /opt/cms-backend/cms-backend.jar
```

---

### Step 3: Crea Directory Upload

```bash
# Crea directory
sudo mkdir -p /var/cms/uploads
sudo mkdir -p /var/cms/avatars

# Imposta permessi
sudo chown -R cms:cms /var/cms
sudo chmod -R 755 /var/cms
```

---

### Step 4: Database Production

**Setup database**:
```sql
-- Crea database
CREATE DATABASE cms_prod;

-- Crea utente dedicato
CREATE USER cms_user WITH PASSWORD 'secure_password';

-- Concedi permessi
GRANT ALL PRIVILEGES ON DATABASE cms_prod TO cms_user;

-- Connettiti al database
\c cms_prod

-- Concedi permessi su schema public
GRANT ALL ON SCHEMA public TO cms_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cms_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO cms_user;
```

**IMPORTANTE in produzione**:
- `ddl-auto: validate` - NON permette modifiche schema automatiche
- Usa migration tools come Flyway o Liquibase per modifiche schema

---

### Step 5: Build e Deploy

```bash
# Build JAR
./gradlew clean build

# Il JAR è in: build/libs/cms-backend-0.0.1-SNAPSHOT.jar

# Copia su server
scp build/libs/cms-backend-0.0.1-SNAPSHOT.jar user@server:/opt/cms-backend/

# Avvia su server
ssh user@server
cd /opt/cms-backend
sudo systemctl start cms-backend

# Verifica logs
sudo journalctl -u cms-backend -f
```

---

## 📊 Variabili d'Ambiente - Riferimento Completo

### Database

| Variabile | Dev Default | Prod | Descrizione |
|-----------|-------------|------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cms_db` | Richiesto | URL connessione database |
| `DB_USERNAME` | `postgres` | Richiesto | Username database |
| `DB_PASSWORD` | `Qwertyui123!` | Richiesto | Password database |
| `DB_DDL_AUTO` | `update` | `validate` | Hibernate DDL mode |
| `DB_SHOW_SQL` | `true` | `false` | Log query SQL |

---

### Media Upload

| Variabile | Dev Default | Prod | Descrizione |
|-----------|-------------|------|-------------|
| `MEDIA_UPLOAD_DIR` | `./uploads` | Richiesto | Directory per immagini content |
| `MEDIA_BASE_URL` | `http://localhost:8080/api/media` | Richiesto | URL pubblico per immagini |

---

### Avatar Upload

| Variabile | Dev Default | Prod | Descrizione |
|-----------|-------------|------|-------------|
| `AVATAR_UPLOAD_DIR` | `./avatars` | Richiesto | Directory per avatar utenti |
| `AVATAR_BASE_URL` | `http://localhost:8080/api/users/avatars` | Richiesto | URL pubblico per avatar |

---

### JWT

| Variabile | Dev Default | Prod | Descrizione |
|-----------|-------------|------|-------------|
| `JWT_SECRET` | (fornito) | Richiesto | Secret per firmare token (min 512 bits) |
| `JWT_EXPIRATION` | `86400000` | `86400000` | Durata token (ms) - 24 ore |

---

### Server

| Variabile | Dev Default | Prod | Descrizione |
|-----------|-------------|------|-------------|
| `SERVER_PORT` | `8080` | `8080` | Porta server |

---

## 🔍 Verifica Configurazione

### Controllo Variabili Caricati

Aggiungi questo al codice per debug (rimuovere in prod!):

```kotlin
@Component
class ConfigCheck(
    @Value("\${spring.datasource.url}") val dbUrl: String,
    @Value("\${jwt.secret}") val jwtSecret: String,
    @Value("\${media.upload.directory}") val mediaDir: String
) {
    @PostConstruct
    fun printConfig() {
        println("=== CONFIG CHECK ===")
        println("DB URL: $dbUrl")
        println("JWT Secret length: ${jwtSecret.length}")
        println("Media Dir: $mediaDir")
        println("===================")
    }
}
```

---

### Verifica Profile Attivo

```bash
# Nei log Spring Boot all'avvio, cerca:
# "The following profiles are active: dev"
# oppure
# "The following profiles are active: prod"
```

---

### Test Connessione Database

```bash
# Test connessione
psql -h localhost -U cms_user -d cms_prod

# Verifica tabelle create
\dt
```

---

## 🔐 Sicurezza Best Practices

### ✅ DA FARE

1. **Secret separati per env**: Genera JWT secret diverso per prod
2. **Password forti**: Database password robusta in prod
3. **File .env fuori repo**: Mai committare `.env` (già in `.gitignore`)
4. **Permessi filesystem**: Directory upload con permessi corretti
5. **HTTPS in prod**: Usa `https://` nei `base-url` in produzione
6. **ddl-auto validate**: Mai usare `update` in prod!

---

### ❌ DA NON FARE

1. ❌ Committare `.env` nel repository
2. ❌ Usare secret di dev in produzione
3. ❌ Hardcodare password in `application-prod.yml`
4. ❌ Esporre porte database pubblicamente
5. ❌ Usare `ddl-auto: update` in produzione
6. ❌ Loggare SQL in produzione (`show-sql: true`)

---

## 🐳 Docker/Kubernetes

### Docker Compose

```yaml
version: '3.8'
services:
  cms-backend:
    image: cms-backend:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:postgresql://db:5432/cms_prod
      - DB_USERNAME=cms_user
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - MEDIA_UPLOAD_DIR=/app/uploads
      - MEDIA_BASE_URL=https://api.example.com/api/media
      - AVATAR_UPLOAD_DIR=/app/avatars
      - AVATAR_BASE_URL=https://api.example.com/api/users/avatars
    volumes:
      - ./uploads:/app/uploads
      - ./avatars:/app/avatars
    ports:
      - "8080:8080"
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=cms_prod
      - POSTGRES_USER=cms_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
```

---

### Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: cms-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DB_URL: "jdbc:postgresql://postgres-service:5432/cms_prod"
  MEDIA_UPLOAD_DIR: "/app/uploads"
  MEDIA_BASE_URL: "https://api.example.com/api/media"
  AVATAR_UPLOAD_DIR: "/app/avatars"
  AVATAR_BASE_URL: "https://api.example.com/api/users/avatars"

---
apiVersion: v1
kind: Secret
metadata:
  name: cms-secrets
type: Opaque
stringData:
  DB_USERNAME: "cms_user"
  DB_PASSWORD: "secure_password"
  JWT_SECRET: "your_base64_secret_here"
```

---

## 📝 Checklist Deploy Produzione

Prima di deployare in produzione:

- [ ] Generato nuovo JWT_SECRET (non usare quello di dev)
- [ ] Database di prod configurato e raggiungibile
- [ ] Utente database con permessi corretti
- [ ] Directory upload create con permessi corretti
- [ ] File .env con tutte le variabili configurate
- [ ] `ddl-auto` impostato a `validate`
- [ ] `show-sql` impostato a `false`
- [ ] `base-url` usano HTTPS (non HTTP)
- [ ] Firewall configurato (solo porte necessarie)
- [ ] SSL/TLS certificato configurato
- [ ] Backup database schedulato
- [ ] Log monitoring configurato
- [ ] Testato che l'app parta con profilo prod
- [ ] Verificato che non ci siano secret hardcoded nel codice

---

## 🆘 Troubleshooting

### Errore: "Property 'DB_URL' is required"

**Causa**: Variabile d'ambiente non configurata per profilo prod.

**Soluzione**: Configura tutte le variabili richieste o usa profilo dev.

---

### Errore: "Failed to configure a DataSource"

**Causa**: Configurazione database errata o database non raggiungibile.

**Debug**:
```bash
# Testa connessione database
psql -h your-db-host -U cms_user -d cms_prod

# Verifica variabili
echo $DB_URL
echo $DB_USERNAME
```

---

### Errore: "Could not create upload directory"

**Causa**: Permessi insufficienti sulla directory.

**Soluzione**:
```bash
# Verifica permessi
ls -la /var/cms/

# Correggi ownership
sudo chown -R cms:cms /var/cms
sudo chmod -R 755 /var/cms
```

---

### App non riconosce profile

**Causa**: Profile non specificato o variabile SPRING_PROFILES_ACTIVE non impostata.

**Verifica**:
```bash
# Nei log all'avvio, cerca:
"The following profiles are active: prod"

# Se vedi "No active profile", il profile non è impostato
```

**Soluzione**:
```bash
# Specifica esplicitamente
java -jar cms-backend.jar --spring.profiles.active=prod

# Oppure via variabile
export SPRING_PROFILES_ACTIVE=prod
java -jar cms-backend.jar
```

---

## 📚 Risorse

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12 Factor App Config](https://12factor.net/config)
