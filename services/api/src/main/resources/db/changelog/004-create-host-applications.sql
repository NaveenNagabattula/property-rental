--liquibase formatted sql

--changeset propertyrental:004-create-host-applications
CREATE TABLE host_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000),
    created_by VARCHAR(50),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_host_applications_user FOREIGN KEY (user_id) REFERENCES users (id)
);
