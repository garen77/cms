# CMS Backend - Spring Boot con Kotlin

Backend RESTful API per un Content Management System sviluppato con Spring Boot e Kotlin.

## Tecnologie Utilizzate

- **Kotlin** 1.9.20
- **Spring Boot** 3.2.0
- **Spring Data JPA**
- **Spring Security** con JWT
- **PostgreSQL**
- **Gradle** (Kotlin DSL)

## Prerequisiti

- JDK 17 o superiore
- PostgreSQL 12 o superiore
- Gradle 8.x (o usa il wrapper incluso)

## Configurazione Database

1. Crea un database PostgreSQL:
```sql
CREATE DATABASE cms_db;
```

2. Esegui lo script SQL per creare le tabelle:
```bash
psql -U postgres -d cms_db -f cms_database.sql
```

3. Modifica `src/main/resources/application.yml` con le tue credenziali:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cms_db
    username: tuo_username
    password: tua_password
```

4. **IMPORTANTE**: Cambia il secret JWT in `application.yml`:
```yaml
jwt:
  secret: tua-chiave-segreta-lunga-almeno-256-bit
```

## Installazione e Avvio

### Usando Gradle Wrapper (consigliato)

```bash
# Su Linux/Mac
./gradlew bootRun

# Su Windows
gradlew.bat bootRun
```

### Usando Gradle installato

```bash
gradle bootRun
```

L'applicazione partirà su `http://localhost:8080`

## Build del Progetto

```bash
# Crea il JAR eseguibile
./gradlew build

# Il JAR sarà in build/libs/cms-backend-0.0.1-SNAPSHOT.jar

# Esegui il JAR
java -jar build/libs/cms-backend-0.0.1-SNAPSHOT.jar
```

## Endpoint API Principali

### Autenticazione
- `POST /api/auth/register` - Registrazione nuovo utente
- `POST /api/auth/login` - Login utente

### Contenuti (Pubblici)
- `GET /api/contents` - Lista contenuti pubblicati (con paginazione)
- `GET /api/contents/{slug}` - Dettaglio contenuto per slug

### Contenuti (Autenticati)
- `POST /api/contents` - Crea nuovo contenuto
- `PUT /api/contents/{id}` - Aggiorna contenuto
- `DELETE /api/contents/{id}` - Elimina contenuto

### Categorie
- `GET /api/categories` - Lista tutte le categorie
- `GET /api/categories/{id}` - Dettaglio categoria
- `POST /api/categories` - Crea categoria (autenticato)
- `PUT /api/categories/{id}` - Aggiorna categoria (autenticato)
- `DELETE /api/categories/{id}` - Elimina categoria (autenticato)

## Esempio di Utilizzo

### Registrazione
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mario",
    "email": "mario@example.com",
    "password": "password123",
    "firstName": "Mario",
    "lastName": "Rossi"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mario",
    "password": "password123"
  }'
```

Risposta:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "username": "mario",
  "email": "mario@example.com",
  "role": "AUTHOR"
}
```

### Creare un Contenuto (con token JWT)
```bash
curl -X POST http://localhost:8080/api/contents \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "Il mio primo articolo",
    "slug": "il-mio-primo-articolo",
    "categoryId": 1,
    "excerpt": "Breve descrizione",
    "body": "Contenuto completo dell'articolo...",
    "status": "PUBLISHED",
    "tags": ["java", "tutorial"]
  }'
```

## Struttura del Progetto

```
src/main/kotlin/com/example/cms/
├── config/              # Configurazioni (Security, JWT, CORS)
├── controller/          # REST Controllers
├── dto/                 # Data Transfer Objects
├── model/               # Entità JPA
├── repository/          # Repository JPA
├── service/             # Business Logic
└── CmsApplication.kt    # Main application
```

## Ruoli Utente

- **ADMIN**: Accesso completo
- **EDITOR**: Può gestire tutti i contenuti
- **AUTHOR**: Può gestire solo i propri contenuti
- **SUBSCRIBER**: Solo lettura

## Sicurezza

- Le password sono criptate con BCrypt
- Autenticazione JWT con token validi per 24 ore
- CORS configurato per accettare richieste da `http://localhost:4200`
- Endpoint pubblici: GET su contenuti, categorie, tag
- Endpoint protetti: Creazione, modifica, eliminazione

## Test

```bash
# Esegui i test
./gradlew test
```

## Troubleshooting

### Errore di connessione al database
Verifica che PostgreSQL sia in esecuzione e che le credenziali siano corrette.

### Errore "JWT signature does not match"
Assicurati di usare lo stesso secret JWT sia per la generazione che per la validazione.

### Port 8080 già in uso
Cambia la porta in `application.yml`:
```yaml
server:
  port: 8081
```

## Sviluppo

Per abilitare il live reload durante lo sviluppo:

```bash
./gradlew bootRun --continuous
```

## License

Questo progetto è open source e disponibile sotto la licenza MIT.
