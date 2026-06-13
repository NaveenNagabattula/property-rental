package com.propertyrental.api.mapper;

import com.propertyrental.api.dto.response.BookingResponse;
import com.propertyrental.api.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "propertyId", source = "property.id")
    @Mapping(target = "propertyTitle", source = "property.title")
    @Mapping(target = "propertyAddress", source = "property.address")
    @Mapping(target = "thumbnailUrl", expression = "java(booking.getProperty().getPhotoUrls() != null && !booking.getProperty().getPhotoUrls().isEmpty() ? booking.getProperty().getPhotoUrls().get(0) : null)")
    @Mapping(target = "guestId", source = "guest.id")
    @Mapping(target = "guestFirstName", source = "guest.firstName")
    @Mapping(target = "guestLastName", source = "guest.lastName")
    @Mapping(target = "createdDate", source = "createdDate")
    BookingResponse toDto(Booking booking);

    /** Convert Instant (UTC) to LocalDateTime (UTC) for the DTO. */
    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}

