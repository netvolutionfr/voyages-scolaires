package fr.siovision.voyages.application.security;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JpaUserDetailsService implements UserDetailsService {


    private final UserRepository users;

    public JpaUserDetailsService(UserRepository users) {
        this.users = users;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown email: " + email));

        // u.getRole() est un enum UserRole { PARENT, STUDENT, ADMIN } -> on sort une seule authority
        return new UserPrincipal(u.getEmail(), u.getRole() != null ? u.getRole().name() : "USER");
    }
}
