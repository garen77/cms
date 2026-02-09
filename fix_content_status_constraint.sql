-- Script per correggere il vincolo di controllo dello status nella tabella contents
-- Esegui questo script se il problema persiste dopo aver riavviato l'applicazione

-- 1. Rimuovi il vecchio constraint (se esiste)
ALTER TABLE contents DROP CONSTRAINT IF EXISTS contents_status_check;

-- 2. Aggiungi il nuovo constraint con i valori corretti (MAIUSCOLI)
ALTER TABLE contents ADD CONSTRAINT contents_status_check
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

-- 3. Verifica il constraint
SELECT constraint_name, check_clause
FROM information_schema.check_constraints
WHERE constraint_name = 'contents_status_check';

-- 4. (OPZIONALE) Se hai dati esistenti con valori in minuscolo, convertili
-- ATTENZIONE: Esegui solo se necessario!
-- UPDATE contents SET status = UPPER(status) WHERE status IN ('draft', 'published', 'archived');
