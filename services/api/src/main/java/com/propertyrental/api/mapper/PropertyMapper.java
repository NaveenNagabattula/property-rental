package com.propertyrental.api.mapper;

import com.propertyrental.api.dto.request.CreatePropertyRequest;
import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    @Mapping(target = "thumbnailUrl", expression = "java(property.getPhotoUrls() != null && !property.getPhotoUrls().isEmpty() ? property.getPhotoUrls().get(0) : null)")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(source = "propertyType", target = "propertyType", qualifiedByName = "enumToString")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    PropertySummaryResponse toSummaryDto(Property property);

    @Mapping(source = "host.id", target = "hostId")
    @Mapping(source = "host.firstName", target = "hostFirstName")
    @Mapping(source = "host.lastName", target = "hostLastName")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(source = "propertyType", target = "propertyType", qualifiedByName = "enumToString")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    PropertyDetailResponse toDetailDto(Property property);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "host", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "propertyType", expression = "java(com.propertyrental.api.entity.enums.PropertyType.valueOf(request.getPropertyType()))")
    Property toEntity(CreatePropertyRequest request);

    @org.mapstruct.Named("enumToString")
    default String enumToString(Enum<?> e) {
        return e == null ? null : e.name();
    }

    @org.mapstruct.Named("statusToString")
    default String statusToString(Enum<?> e) {
        return e == null ? null : e.name();
    }
}
