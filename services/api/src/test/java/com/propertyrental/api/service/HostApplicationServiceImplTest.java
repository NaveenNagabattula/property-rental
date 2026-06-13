package com.propertyrental.api.service;

import com.propertyrental.api.dto.response.HostApplicationResponse;
import com.propertyrental.api.entity.HostApplication;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.repository.HostApplicationRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.impl.HostApplicationServiceImpl;
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
@DisplayName("HostApplicationServiceImpl Unit Tests")
class HostApplicationServiceImplTest {

    @Mock private HostApplicationRepository hostApplicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private HostApplicationServiceImpl hostApplicationService;

    private User guestUser;
    private User hostUser;
    private HostApplication pendingApp;

    @BeforeEach
    void setUp() {
        guestUser = User.builder()
                .id(UUID.randomUUID())
                .email("guest@example.com")
                .firstName("John")
                .role(Role.GUEST)
                .build();

        hostUser = User.builder()
                .id(UUID.randomUUID())
                .email("host@example.com")
                .firstName("Alice")
                .role(Role.HOST)
                .build();

        pendingApp = HostApplication.builder()
                .id(UUID.randomUUID())
                .user(guestUser)
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("applyForHost — success: creates a pending application")
    void applyForHost_success() {
        given(userRepository.findById(guestUser.getId())).willReturn(Optional.of(guestUser));
        given(hostApplicationRepository.existsByUserIdAndStatus(guestUser.getId(), "PENDING")).willReturn(false);
        given(hostApplicationRepository.save(any(HostApplication.class))).willAnswer(inv -> {
            HostApplication app = inv.getArgument(0);
            app.setId(UUID.randomUUID());
            return app;
        });

        HostApplicationResponse response = hostApplicationService.applyForHost(guestUser.getId());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getUserId()).isEqualTo(guestUser.getId());
        verify(hostApplicationRepository).save(any(HostApplication.class));
    }

    @Test
    @DisplayName("applyForHost — throws BusinessRuleException when user is already a host")
    void applyForHost_alreadyHost_throwsException() {
        given(userRepository.findById(hostUser.getId())).willReturn(Optional.of(hostUser));

        assertThatThrownBy(() -> hostApplicationService.applyForHost(hostUser.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already a Host");

        verify(hostApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyForHost — throws BusinessRuleException when user has a pending application")
    void applyForHost_hasPending_throwsException() {
        given(userRepository.findById(guestUser.getId())).willReturn(Optional.of(guestUser));
        given(hostApplicationRepository.existsByUserIdAndStatus(guestUser.getId(), "PENDING")).willReturn(true);

        assertThatThrownBy(() -> hostApplicationService.applyForHost(guestUser.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending host application");

        verify(hostApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPendingApplications — returns applications in PENDING status")
    void getPendingApplications_success() {
        Page<HostApplication> page = new PageImpl<>(Collections.singletonList(pendingApp));

        given(hostApplicationRepository.findByStatus(eq("PENDING"), any(Pageable.class))).willReturn(page);

        Page<HostApplicationResponse> result = hostApplicationService.getPendingApplications(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("approveApplication — success: status becomes APPROVED, user role is upgraded to HOST, email is sent")
    void approveApplication_success() {
        given(hostApplicationRepository.findById(pendingApp.getId())).willReturn(Optional.of(pendingApp));
        given(hostApplicationRepository.save(any(HostApplication.class))).willAnswer(inv -> inv.getArgument(0));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        HostApplicationResponse response = hostApplicationService.approveApplication(pendingApp.getId());

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(guestUser.getRole()).isEqualTo(Role.HOST);
        verify(hostApplicationRepository).save(pendingApp);
        verify(userRepository).save(guestUser);
        verify(emailService).sendHostApplicationDecisionEmail(guestUser.getEmail(), guestUser.getFirstName(), true, null);
    }

    @Test
    @DisplayName("rejectApplication — success: status becomes REJECTED and email is sent with reason")
    void rejectApplication_success() {
        given(hostApplicationRepository.findById(pendingApp.getId())).willReturn(Optional.of(pendingApp));
        given(hostApplicationRepository.save(any(HostApplication.class))).willAnswer(inv -> inv.getArgument(0));

        HostApplicationResponse response = hostApplicationService.rejectApplication(pendingApp.getId(), "Incomplete profile info");

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getReason()).isEqualTo("Incomplete profile info");
        assertThat(guestUser.getRole()).isEqualTo(Role.GUEST); // Role remains GUEST
        verify(hostApplicationRepository).save(pendingApp);
        verify(emailService).sendHostApplicationDecisionEmail(guestUser.getEmail(), guestUser.getFirstName(), false, "Incomplete profile info");
    }
}
