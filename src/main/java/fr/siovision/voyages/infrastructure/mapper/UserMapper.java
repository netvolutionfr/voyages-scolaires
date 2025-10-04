package fr.siovision.voyages.infrastructure.mapper;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toDTO(User user);
    User toEntity(UserResponse userResponse);
}
