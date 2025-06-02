-- Insertar roles iniciales
INSERT INTO roles (name, description) VALUES 
    ('ROLE_ADMIN', 'Administrador del sistema'),
    ('ROLE_USER', 'Usuario estándar'),
    ('ROLE_READ_ONLY', 'Usuario de solo lectura'),
    ('ROLE_API', 'Acceso solo a APIs'),
    ('ROLE_MODERATOR', 'Moderador de contenido')
ON CONFLICT (name) DO NOTHING;

-- Insertar usuario administrador (contraseña: admin123)
-- La contraseña es el hash BCrypt de "admin123"
INSERT INTO users (username, password, email, first_name, last_name, enabled) 
VALUES ('admin', '$2a$12$h8r0mFfTd6wW1jquPdeWVuHxpRhOw5QYThHguV6DxefU.8KmBkgQe', 'admin@formacionbdi.com', 'Admin', 'System', true)
ON CONFLICT (username) DO NOTHING;

-- Asignar rol de administrador al usuario admin
INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, u.id
FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Insertar un cliente OAuth2 para la aplicación web
INSERT INTO oauth_client_details (
    client_id, 
    client_secret, 
    resource_ids, 
    scope, 
    authorized_grant_types, 
    web_server_redirect_uri, 
    authorities,
    access_token_validity, 
    refresh_token_validity, 
    additional_information, 
    autoapprove, 
    active
) VALUES (
    'gateway-client', 
    '$2a$12$h8r0mFfTd6wW1jquPdeWVuHxpRhOw5QYThHguV6DxefU.8KmBkgQe', -- secret123
    'gateway-service', 
    'read,write', 
    'password,refresh_token,client_credentials,authorization_code', 
    NULL, 
    'ROLE_CLIENT',
    3600, 
    86400, 
    '{}', 
    'true', 
    true
) ON CONFLICT (client_id) DO NOTHING;

-- Insertar un usuario de prueba (contraseña: user123)
-- La contraseña es el hash BCrypt de "user123"
INSERT INTO users (username, password, email, first_name, last_name, enabled)
VALUES ('usuario', '$2a$12$h8r0mFfTd6wW1jquPdeWVuHxpRhOw5QYThHguV6DxefU.8KmBkgQe', 'usuario@formacionbdi.com', 'Usuario', 'Prueba', true)
ON CONFLICT (username) DO NOTHING;

-- Asignar rol de usuario al usuario de prueba
INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, (SELECT id FROM users WHERE username = 'admin')
FROM users u, roles r 
WHERE u.username = 'usuario' AND r.name = 'ROLE_USER'
ON CONFLICT (user_id, role_id) DO NOTHING;
