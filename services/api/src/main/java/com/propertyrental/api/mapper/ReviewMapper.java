package com.propertyrental.api.mapper;

import com.propertyrental.api.dto.response.ReviewResponse;
import com.propertyrental.api.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "propertyId", source = "property.id")
    @Mapping(target = "propertyTitle", source = "property.title")
    @Mapping(target = "guestId", source = "guest.id")
    @Mapping(target = "guestFirstName", source = "guest.firstName")
    @Mapping(target = "guestLastName", source = "guest.lastName")
    ReviewResponse toDto(Review review);
}
