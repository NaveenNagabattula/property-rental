package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.ChangeRoleRequest;
import com.propertyrental.api.dto.response.UserResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserManagementService {

    Page<UserResponse> searchUsers(String queryText, String role, Boolean isActive, int page, int size);

    UserResponse deactivateUser(UUID id);

    UserResponse activateUser(UUID id);

    void deleteUser(UUID id);

    UserResponse changeRole(UUID id, ChangeRoleRequest request);
}
