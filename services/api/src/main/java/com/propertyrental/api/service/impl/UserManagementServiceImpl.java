package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.ChangeRoleRequest;
import com.propertyrental.api.dto.response.UserResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.mapper.UserMapper;
import com.propertyrental.api.repository.RefreshTokenRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.repository.specification.UserSpecification;
import com.propertyrental.api.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> searchUsers(String queryText, String role, Boolean isActive, int page, int size) {
        Role userRole = null;
        if (role != null && !role.isBlank()) {
            try {
                userRole = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("Invalid role filter: " + role);
            }
        }

        Specification<User> spec = Specification
                .where(UserSpecification.hasSearchQuery(queryText))
                .and(UserSpecification.hasRole(userRole))
                .and(UserSpecification.isActive(isActive));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "email"));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        user.setActive(false);
        userRepository.save(user);

        // Invalidate all active sessions by deleting user refresh tokens
        refreshTokenRepository.deleteByUser(user);

        log.info("User {} deactivated and refresh tokens invalidated.", id);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserResponse activateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (user.isDeleted()) {
            throw new BusinessRuleException("Cannot activate a permanently deleted account.");
        }

        if (user.isActive()) {
            throw new BusinessRuleException("User account is already active.");
        }

        user.setActive(true);
        User saved = userRepository.save(user);

        log.info("User {} reactivated by administrator.", id);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (user.isDeleted()) {
            throw new BusinessRuleException("User account has already been deleted.");
        }

        // Revoke all active sessions immediately
        refreshTokenRepository.deleteByUser(user);

        // Soft-delete: anonymize PII and mark as deleted.
        // Hard deletion is not possible because users are referenced by bookings,
        // properties, reviews, and host applications via foreign keys without CASCADE.
        String anonymizedEmail = "deleted_" + id + "@deleted.invalid";
        user.setEmail(anonymizedEmail);
        user.setPasswordHash("[DELETED]");
        user.setFirstName("[Deleted]");
        user.setLastName("[Deleted]");
        user.setForgotPasswordToken(null);
        user.setForgotPasswordExpiry(null);
        user.setActive(false);
        user.setDeleted(true);
        user.setDeletedAt(java.time.Instant.now());

        userRepository.save(user);
        log.info("User {} soft-deleted and PII anonymized by administrator.", id);
    }

    @Override
    @Transactional
    public UserResponse changeRole(UUID id, ChangeRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        Role newRole;
        try {
            newRole = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid role value: " + request.getRole());
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);
        
        log.info("User {} role changed to {} by administrator.", id, newRole);
        return userMapper.toDto(saved);
    }
}
