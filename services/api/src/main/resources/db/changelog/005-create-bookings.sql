--liquibase formatted sql

--changeset propertyrental:005-create-bookings
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    guest_id UUID NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guest_count INTEGER NOT NULL,
    total_price NUMERIC(12,2) NOT NULL,
    platform_fee NUMERIC(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    special_requests VARCHAR(1000),
    cancellation_reason VARCHAR(1000),
    created_by VARCHAR(50),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_bookings_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_bookings_guest FOREIGN KEY (guest_id) REFERENCES users (id)
);

CREATE TABLE availability_blocks (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    blocked_date DATE NOT NULL,
    source VARCHAR(50) NOT NULL,
    CONSTRAINT fk_avail_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_availability_blocks_property_date
    ON availability_blocks (property_id, blocked_date);
