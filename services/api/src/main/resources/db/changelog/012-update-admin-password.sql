--liquibase formatted sql

--changeset propertyrental:012-update-admin-password
UPDATE users 
SET password_hash = '$2a$12$Ugr9Ap3k3HWc13bb6uJHuej3JQLlJ9yH3nrfdP.UAnP5NPAn3bbvO' 
WHERE email = 'admin@propertyrental.com';
