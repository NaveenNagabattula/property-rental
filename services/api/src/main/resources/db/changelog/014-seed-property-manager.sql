--liquibase formatted sql

--changeset propertyrental:014-seed-property-manager
-- Default property manager account for dev/staging environments
-- Password: admin123  (BCrypt cost 12 — same verified hash as admin user)
-- IMPORTANT: Change this password immediately after first login
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
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'manager@propertyrental.com',
    '$2a$12$Ugr9Ap3k3HWc13bb6uJHuej3JQLlJ9yH3nrfdP.UAnP5NPAn3bbvO',
    'Property',
    'Manager',
    'PROPERTY_MANAGER',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
