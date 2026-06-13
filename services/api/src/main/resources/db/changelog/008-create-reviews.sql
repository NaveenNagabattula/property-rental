--liquibase formatted sql

--changeset propertyrental:008-create-reviews
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    property_id UUID NOT NULL,
    guest_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(2000),
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_reviews_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_reviews_guest FOREIGN KEY (guest_id) REFERENCES users (id)
);
