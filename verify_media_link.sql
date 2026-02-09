-- Script per verificare il collegamento tra contents e media

-- 1. Verifica la struttura della tabella contents
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'contents' AND column_name = 'featured_image_id';

-- 2. Verifica i contents con le loro immagini
SELECT
    c.id as content_id,
    c.title,
    c.slug,
    c.featured_image_id,
    m.id as media_id,
    m.filename,
    m.original_filename,
    m.file_url
FROM contents c
LEFT JOIN media m ON c.featured_image_id = m.id
ORDER BY c.id DESC
LIMIT 10;

-- 3. Verifica quanti contents hanno immagini
SELECT
    COUNT(*) as total_contents,
    COUNT(featured_image_id) as contents_with_image,
    COUNT(*) - COUNT(featured_image_id) as contents_without_image
FROM contents;

-- 4. Lista tutte le immagini caricate
SELECT
    id,
    filename,
    original_filename,
    file_url,
    created_at
FROM media
ORDER BY created_at DESC
LIMIT 10;

-- 5. Verifica se esiste la tabella content_media (non dovrebbe servire)
SELECT table_name
FROM information_schema.tables
WHERE table_name = 'content_media';
