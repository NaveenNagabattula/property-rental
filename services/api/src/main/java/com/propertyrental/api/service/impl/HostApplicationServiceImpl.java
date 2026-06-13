package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.response.HostApplicationResponse;
import com.propertyrental.api.entity.HostApplication;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.repository.HostApplicationRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.EmailService;
import com.propertyrental.api.service.HostApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostApplicationServiceImpl implements HostApplicationService {

    private final HostApplicationRepository hostApplicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public HostApplicationResponse applyForHost(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getRole() == Role.HOST) {
            throw new BusinessRuleException("You are already a Host");
        }

        boolean hasPending = hostApplicationRepository.existsByUserIdAndStatus(userId, "PENDING");
        if (hasPending) {
            throw new BusinessRuleException("You already have a pending host application");
        }

        HostApplication app = HostApplication.builder()
                .user(user)
                .status("PENDING")
                .build();

        HostApplication saved = hostApplicationRepository.save(app);
        log.info("Host application submitted for userId={}", userId);
        return toResponse(saved);
    }

    @Override
    public Page<HostApplicationResponse> getPendingApplications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return hostApplicationRepository.findByStatus("PENDING", pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public HostApplicationResponse approveApplication(UUID id) {
        HostApplication app = hostApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Host application not found with id: " + id));

        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessRuleException("Application is not in PENDING state (current: " + app.getStatus() + ")");
        }

        app.setStatus("APPROVED");
        User user = app.getUser();
        user.setRole(Role.HOST);
        userRepository.save(user);
        HostApplication saved = hostApplicationRepository.save(app);

        emailService.sendHostApplicationDecisionEmail(user.getEmail(), user.getFirstName(), true, null);
        log.info("Host application {} approved. User {} upgraded to HOST.", id, user.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public HostApplicationResponse rejectApplication(UUID id, String reason) {
        HostApplication app = hostApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Host application not found with id: " + id));

        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessRuleException("Application is not in PENDING state (current: " + app.getStatus() + ")");
        }

        app.setStatus("REJECTED");
        app.setReason(reason);
        HostApplication saved = hostApplicationRepository.save(app);

        User user = app.getUser();
        emailService.sendHostApplicationDecisionEmail(user.getEmail(), user.getFirstName(), false, reason);
        log.info("Host application {} rejected with reason: {}", id, reason);
        return toResponse(saved);
    }

    private HostApplicationResponse toResponse(HostApplication app) {
        return HostApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUser().getId())
                .userEmail(app.getUser().getEmail())
                .userFirstName(app.getUser().getFirstName())
                .userLastName(app.getUser().getLastName())
                .status(app.getStatus())
                .reason(app.getReason())
                .createdDate(app.getCreatedDate())
                .build();
    }
}
