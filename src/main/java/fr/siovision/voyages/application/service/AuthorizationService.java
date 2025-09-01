package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    public boolean hasAnyRole(User user, UserRole... roles) {
        if (user == null || roles == null) {
            return true;
        }
        for (UserRole role : roles) {
            if (user.getRole() != null && user.getRole().equals(role)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRole(User current, UserRole userRole) {
        if (current == null || userRole == null) {
            return false;
        }
        return current.getRole() != null && current.getRole().equals(userRole);
    }
}
