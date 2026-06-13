package com.propertyrental.api.service;

import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.PropertyStatus;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.mapper.PropertyMapper;
import com.propertyrental.api.repository.PropertyRepository;
import com.propertyrental.api.service.impl.AdminPropertyServiceImpl;
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
@DisplayName("AdminPropertyServiceImpl Unit Tests")
class AdminPropertyServiceImplTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PropertyMapper propertyMapper;
    @Mock private EmailService emailService;

    @InjectMocks
    private AdminPropertyServiceImpl adminPropertyService;

    private Property pendingProperty;
    private Property activeProperty;
    private User host;

    @BeforeEach
    void setUp() {
        host = User.builder()
                .id(UUID.randomUUID())
                .email("host@example.com")
                .firstName("Alice")
                .build();

        pendingProperty = Property.builder()
                .id(UUID.randomUUID())
                .title("Mountain Cabin")
                .host(host)
                .status(PropertyStatus.PENDING_REVIEW)
                .build();

        activeProperty = Property.builder()
                .id(UUID.randomUUID())
                .title("Luxury Villa")
                .host(host)
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("getPendingListings — returns listings with PENDING_REVIEW status")
    void getPendingListings_success() {
        Page<Property> page = new PageImpl<>(Collections.singletonList(pendingProperty));
        
        given(propertyRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(page);
        given(propertyMapper.toSummaryDto(pendingProperty)).willReturn(
                PropertySummaryResponse.builder().id(pendingProperty.getId()).status("PENDING_REVIEW").build()
        );

        Page<PropertySummaryResponse> result = adminPropertyService.getPendingListings(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    @DisplayName("approveListing — success: transitions status to ACTIVE and sends email")
    void approveListing_success() {
        given(propertyRepository.findById(pendingProperty.getId())).willReturn(Optional.of(pendingProperty));
        given(propertyRepository.save(any(Property.class))).willAnswer(inv -> inv.getArgument(0));
        given(propertyMapper.toDetailDto(any(Property.class))).willReturn(
                PropertyDetailResponse.builder().id(pendingProperty.getId()).status("ACTIVE").build()
        );

        PropertyDetailResponse response = adminPropertyService.approveListing(pendingProperty.getId());

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(pendingProperty.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
        verify(propertyRepository).save(pendingProperty);
        verify(emailService).sendPropertyModerationDecisionEmail(
                eq(host.getEmail()), eq(host.getFirstName()), eq(pendingProperty.getTitle()), eq(true), any()
        );
    }

    @Test
    @DisplayName("approveListing — throws BusinessRuleException if listing is not PENDING_REVIEW")
    void approveListing_invalidState_throwsException() {
        given(propertyRepository.findById(activeProperty.getId())).willReturn(Optional.of(activeProperty));

        assertThatThrownBy(() -> adminPropertyService.approveListing(activeProperty.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING_REVIEW");

        verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectListing — success: transitions status to DRAFT and sends email with reason")
    void rejectListing_success() {
        given(propertyRepository.findById(pendingProperty.getId())).willReturn(Optional.of(pendingProperty));
        given(propertyRepository.save(any(Property.class))).willAnswer(inv -> inv.getArgument(0));
        given(propertyMapper.toDetailDto(any(Property.class))).willReturn(
                PropertyDetailResponse.builder().id(pendingProperty.getId()).status("DRAFT").build()
        );

        PropertyDetailResponse response = adminPropertyService.rejectListing(pendingProperty.getId(), "Incomplete details");

        assertThat(response.getStatus()).isEqualTo("DRAFT");
        assertThat(pendingProperty.getStatus()).isEqualTo(PropertyStatus.DRAFT);
        verify(propertyRepository).save(pendingProperty);
        verify(emailService).sendPropertyModerationDecisionEmail(
                eq(host.getEmail()), eq(host.getFirstName()), eq(pendingProperty.getTitle()), eq(false), eq("Incomplete details")
        );
    }

    @Test
    @DisplayName("suspendListing — success: transitions active status to SUSPENDED")
    void suspendListing_success() {
        given(propertyRepository.findById(activeProperty.getId())).willReturn(Optional.of(activeProperty));
        given(propertyRepository.save(any(Property.class))).willAnswer(inv -> inv.getArgument(0));
        given(propertyMapper.toDetailDto(any(Property.class))).willReturn(
                PropertyDetailResponse.builder().id(activeProperty.getId()).status("SUSPENDED").build()
        );

        PropertyDetailResponse response = adminPropertyService.suspendListing(activeProperty.getId(), "Violation of terms");

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
        assertThat(activeProperty.getStatus()).isEqualTo(PropertyStatus.SUSPENDED);
        verify(propertyRepository).save(activeProperty);
        verify(emailService).sendPropertyModerationDecisionEmail(
                eq(host.getEmail()), eq(host.getFirstName()), eq(activeProperty.getTitle()), eq(false), eq("Suspended: Violation of terms")
        );
    }
}
