package com.propertyrental.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private UUID propertyId;
    private String propertyTitle;
    private String propertyAddress;
    private String thumbnailUrl;
    private UUID guestId;
    private String guestFirstName;
    private String guestLastName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private BigDecimal totalPrice;
    private BigDecimal platformFee;
    private String status;
    private String specialRequests;
    private String cancellationReason;
    private LocalDateTime createdDate;
}
