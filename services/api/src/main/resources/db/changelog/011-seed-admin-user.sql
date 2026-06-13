--liquibase formatted sql

--changeset propertyrental:011-seed-admin-user
INSERT INTO users (
    id, 
    email, 
    password_hash, 
    first_name, 
    last_name, 
    role, 
    is_active, 
    is_email_verified, 
    created_date, 
    last_modified_date
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'admin@propertyrental.com', 
    '$2a$12$mC5zF2G8.P8x1y3q7Lh6eeG7r09K1r2R6.cE1jG.gQ8V2vU2g3W3m', -- BCrypt hash of 'admin123' with cost 12
    'Admin', 
    'Console', 
    'SUPER_ADMIN', 
    TRUE, 
    TRUE, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
