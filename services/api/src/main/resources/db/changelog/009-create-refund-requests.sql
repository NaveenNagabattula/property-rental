--liquibase formatted sql

--changeset propertyrental:009-create-refund-requests
CREATE TABLE refund_requests (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    refund_amount NUMERIC(12,2),
    resolved_by UUID,
    resolution_notes VARCHAR(1000),
    created_by VARCHAR(50),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_refund_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_refund_requester FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_refund_resolver FOREIGN KEY (resolved_by) REFERENCES users (id)
);
