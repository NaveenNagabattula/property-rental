package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.CreatePropertyRequest;
import com.propertyrental.api.dto.request.PropertySearchRequest;
import com.propertyrental.api.dto.request.UpdatePropertyRequest;
import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.PropertyStatus;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.mapper.PropertyMapper;
import com.propertyrental.api.repository.PropertyRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.impl.PropertyServiceImpl;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
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
@DisplayName("PropertyServiceImpl Unit Tests")
class PropertyServiceImplTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyMapper propertyMapper;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private User host;
    private User guest;
    private Property property;
    private CreatePropertyRequest createRequest;

    @BeforeEach
    void setUp() {
        host = User.builder()
                .id(UUID.randomUUID())
                .email("host@example.com")
                .firstName("Alice")
                .role(Role.HOST)
                .build();

        guest = User.builder()
                .id(UUID.randomUUID())
                .email("guest@example.com")
                .firstName("John")
                .role(Role.GUEST)
                .build();

        property = Property.builder()
                .id(UUID.randomUUID())
                .host(host)
                .title("Modern Apartment")
                .status(PropertyStatus.DRAFT)
                .pricePerNight(BigDecimal.valueOf(150))
                .guestCapacity(2)
                .build();

        createRequest = CreatePropertyRequest.builder()
                .title("Modern Apartment")
                .description("Nice place")
                .address("123 Street")
                .latitude(12.34)
                .longitude(56.78)
                .pricePerNight(BigDecimal.valueOf(150))
                .guestCapacity(2)
                .propertyType("APARTMENT")
                .build();
    }

    @Test
    @DisplayName("searchProperties — returns paginated list of properties matching specs")
    void searchProperties_success() {
        PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                .location("New York")
                .guests(2)
                .page(0)
                .size(12)
                .sortBy("pricePerNight")
                .sortDir("asc")
                .build();

        PageRequest pageRequest = PageRequest.of(0, 12, Sort.by(Sort.Direction.ASC, "pricePerNight"));
        Page<Property> page = new PageImpl<>(Collections.singletonList(property));

        given(propertyRepository.findAll(any(Specification.class), eq(pageRequest))).willReturn(page);
        given(propertyMapper.toSummaryDto(property)).willReturn(
                PropertySummaryResponse.builder().id(property.getId()).title(property.getTitle()).build()
        );

        Page<PropertySummaryResponse> result = propertyService.searchProperties(searchRequest);

        assertThat(result.getContent()).hasSize(1);
        verify(propertyRepository).findAll(any(Specification.class), eq(pageRequest));
    }

    @Test
    @DisplayName("getPropertyById — success: returns detail DTO")
    void getPropertyById_success() {
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));
        given(propertyMapper.toDetailDto(property)).willReturn(
                PropertyDetailResponse.builder().id(property.getId()).title(property.getTitle()).build()
        );

        PropertyDetailResponse result = propertyService.getPropertyById(property.getId());

        assertThat(result.getId()).isEqualTo(property.getId());
    }

    @Test
    @DisplayName("getPropertyById — throws ResourceNotFoundException for invalid ID")
    void getPropertyById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        given(propertyRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.getPropertyById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createProperty — success: saves draft property and returns detail")
    void createProperty_success() {
        given(userRepository.findById(host.getId())).willReturn(Optional.of(host));
        given(propertyMapper.toEntity(createRequest)).willReturn(property);
        given(propertyRepository.save(any(Property.class))).willAnswer(inv -> inv.getArgument(0));
        given(propertyMapper.toDetailDto(any(Property.class))).willReturn(
                PropertyDetailResponse.builder().id(property.getId()).status("DRAFT").build()
        );

        PropertyDetailResponse response = propertyService.createProperty(createRequest, host.getId());

        assertThat(response.getStatus()).isEqualTo("DRAFT");
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    @DisplayName("createProperty — throws UnauthorizedException if user is not HOST role")
    void createProperty_nonHost_throwsException() {
        given(userRepository.findById(guest.getId())).willReturn(Optional.of(guest));

        assertThatThrownBy(() -> propertyService.createProperty(createRequest, guest.getId()))
                .isInstanceOf(UnauthorizedException.class);

        verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitForReview — success: transitions DRAFT to PENDING_REVIEW")
    void submitForReview_success() {
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));
        given(propertyRepository.save(any(Property.class))).willAnswer(inv -> inv.getArgument(0));
        given(propertyMapper.toDetailDto(any(Property.class))).willReturn(
                PropertyDetailResponse.builder().id(property.getId()).status("PENDING_REVIEW").build()
        );

        PropertyDetailResponse response = propertyService.submitForReview(property.getId(), host.getId());

        assertThat(response.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.PENDING_REVIEW);
        verify(propertyRepository).save(property);
    }

    @Test
    @DisplayName("submitForReview — throws BusinessRuleException if not in DRAFT status")
    void submitForReview_invalidState_throwsException() {
        property.setStatus(PropertyStatus.ACTIVE);
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));

        assertThatThrownBy(() -> propertyService.submitForReview(property.getId(), host.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only DRAFT properties");

        verify(propertyRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProperty — success: owner updates listing")
    void updateProperty_success() {
        UpdatePropertyRequest updateRequest = UpdatePropertyRequest.builder()
                .title("Newly Renovated Apartment")
                .build();

        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));
        given(userRepository.findById(host.getId())).willReturn(Optional.of(host));
        given(propertyRepository.save(any(Property.class))).willAnswer(inv -> inv.getArgument(0));
        given(propertyMapper.toDetailDto(any(Property.class))).willReturn(
                PropertyDetailResponse.builder().id(property.getId()).title("Newly Renovated Apartment").build()
        );

        PropertyDetailResponse response = propertyService.updateProperty(property.getId(), updateRequest, host.getId());

        assertThat(response.getTitle()).isEqualTo("Newly Renovated Apartment");
        verify(propertyRepository).save(property);
    }
}
