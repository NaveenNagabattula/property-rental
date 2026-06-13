--liquibase formatted sql

--changeset propertyrental:010-add-audit-to-availability-blocks
ALTER TABLE availability_blocks 
    ADD COLUMN created_by VARCHAR(50),
    ADD COLUMN created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ADD COLUMN last_modified_by VARCHAR(50),
    ADD COLUMN last_modified_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
