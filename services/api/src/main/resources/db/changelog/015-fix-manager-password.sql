--liquibase formatted sql

--changeset propertyrental:015-fix-manager-password
-- Fix: update property manager password hash to verified BCrypt hash for 'admin123'
UPDATE users
SET password_hash = '$2a$12$Ugr9Ap3k3HWc13bb6uJHuej3JQLlJ9yH3nrfdP.UAnP5NPAn3bbvO'
WHERE email = 'manager@propertyrental.com';
