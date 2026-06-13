package com.propertyrental.api.repository.specification;

import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> hasSearchQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) return Specification.where(null);
        String pattern = "%" + queryText.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern)
        );
    }

    public static Specification<User> hasRole(Role role) {
        if (role == null) return Specification.where(null);
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<User> isActive(Boolean isActive) {
        if (isActive == null) return Specification.where(null);
        return (root, query, cb) -> cb.equal(root.get("active"), isActive);
    }
}
