package com.propertyrental.api.repository.specification;

import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.enums.PropertyStatus;
import com.propertyrental.api.entity.enums.PropertyType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class PropertySpecification {

    private PropertySpecification() {}

    public static Specification<Property> hasLocation(String location) {
        if (location == null || location.isBlank()) return Specification.where(null);
        String pattern = "%" + location.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("address")), pattern),
                cb.like(cb.lower(root.get("title")), pattern)
        );
    }

    public static Specification<Property> hasGuestCapacity(Integer guests) {
        if (guests == null) return Specification.where(null);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("guestCapacity"), guests);
    }

    public static Specification<Property> hasMinPrice(BigDecimal minPrice) {
        if (minPrice == null) return Specification.where(null);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice);
    }

    public static Specification<Property> hasMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) return Specification.where(null);
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice);
    }

    public static Specification<Property> hasPropertyType(PropertyType type) {
        if (type == null) return Specification.where(null);
        return (root, query, cb) -> cb.equal(root.get("propertyType"), type);
    }

    public static Specification<Property> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), PropertyStatus.ACTIVE);
    }

    public static Specification<Property> hasStatus(PropertyStatus status) {
        if (status == null) return Specification.where(null);
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
