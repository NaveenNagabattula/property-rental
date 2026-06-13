--liquibase formatted sql

--changeset propertyrental:003-create-properties
CREATE TABLE properties (
    id UUID PRIMARY KEY,
    host_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    price_per_night NUMERIC(10,2) NOT NULL,
    guest_capacity INTEGER NOT NULL,
    bedroom_count INTEGER NOT NULL,
    bathroom_count INTEGER NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(50),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_properties_host FOREIGN KEY (host_id) REFERENCES users (id)
);

CREATE TABLE property_amenities (
    property_id UUID NOT NULL,
    amenity VARCHAR(100) NOT NULL,
    CONSTRAINT fk_amenities_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

CREATE TABLE property_photos (
    property_id UUID NOT NULL,
    photo_url VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_photos_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);
