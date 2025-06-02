-- Asegurarse de que los campos requeridos estén configurados como NOT NULL
ALTER TABLE users 
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN enabled SET NOT NULL,
    ALTER COLUMN enabled SET DEFAULT true,
    ADD COLUMN IF NOT EXISTS nombre VARCHAR(100),
    ADD COLUMN IF NOT EXISTS apellido VARCHAR(100);

-- Actualizar los campos nombre y apellido si existen los campos first_name y last_name
UPDATE users SET 
    nombre = first_name,
    apellido = last_name
WHERE (nombre IS NULL OR apellido IS NULL) AND (first_name IS NOT NULL OR last_name IS NOT NULL);

-- Eliminar las columnas first_name y last_name si existen
ALTER TABLE users 
    DROP COLUMN IF EXISTS first_name,
    DROP COLUMN IF EXISTS last_name;
