package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.ChangeRoleRequest;
import com.propertyrental.api.dto.response.UserResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.mapper.UserMapper;
import com.propertyrental.api.repository.RefreshTokenRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.impl.UserManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementServiceImpl Unit Tests")
class UserManagementServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.GUEST)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("searchUsers — returns users matching specifications")
    void searchUsers_success() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(Collections.singletonList(testUser));

        given(userRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(page);
        given(userMapper.toDto(testUser)).willReturn(
                UserResponse.builder().id(testUser.getId()).email(testUser.getEmail()).build()
        );

        Page<UserResponse> result = userManagementService.searchUsers("Jane", "GUEST", true, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("deactivateUser — success: updates isActive to false and deletes refresh tokens")
    void deactivateUser_success() {
        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(userMapper.toDto(any(User.class))).willReturn(
                UserResponse.builder().id(testUser.getId()).active(false).build()
        );

        UserResponse response = userManagementService.deactivateUser(testUser.getId());

        assertThat(response.isActive()).isFalse();
        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).save(testUser);
        verify(refreshTokenRepository).deleteByUser(testUser);
    }

    @Test
    @DisplayName("deactivateUser — throws ResourceNotFoundException for non-existing user")
    void deactivateUser_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.deactivateUser(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("changeRole — success: upgrades/downgrades user role")
    void changeRole_success() {
        ChangeRoleRequest request = ChangeRoleRequest.builder().role("HOST").build();

        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(userMapper.toDto(any(User.class))).willReturn(
                UserResponse.builder().id(testUser.getId()).role("HOST").build()
        );

        UserResponse response = userManagementService.changeRole(testUser.getId(), request);

        assertThat(response.getRole()).isEqualTo("HOST");
        assertThat(testUser.getRole()).isEqualTo(Role.HOST);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("changeRole — throws BusinessRuleException for invalid role value")
    void changeRole_invalidRole_throwsException() {
        ChangeRoleRequest request = ChangeRoleRequest.builder().role("UNKNOWN_ROLE").build();

        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userManagementService.changeRole(testUser.getId(), request))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
    }
}
