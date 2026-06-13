--liquibase formatted sql

--changeset propertyrental:007-create-platform-config
CREATE TABLE platform_config (
    id UUID PRIMARY KEY,
    config_key VARCHAR(200) NOT NULL UNIQUE,
    config_value VARCHAR(2000) NOT NULL,
    description VARCHAR(500),
    created_by VARCHAR(50),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO platform_config (id, config_key, config_value, description, created_date, last_modified_date)
VALUES
    (gen_random_uuid(), 'platform.fee.percentage', '10', 'Platform fee as % of booking total', NOW(), NOW()),
    (gen_random_uuid(), 'platform.max.guests', '16', 'Maximum guests per booking', NOW(), NOW()),
    (gen_random_uuid(), 'platform.cancellation.grace.hours', '24', 'Hours after booking to cancel for full refund', NOW(), NOW());
