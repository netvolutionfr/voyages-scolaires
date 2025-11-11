package fr.siovision.voyages.infrastructure.mapper;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "section.label", target = "section")
    @Mapping(source = "section.publicId", target = "sectionPublicId")
    @Mapping(target = "fullName", expression = "java(mapFullName(user))")
    UserResponse toDTO(User user);

    default String mapFullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
