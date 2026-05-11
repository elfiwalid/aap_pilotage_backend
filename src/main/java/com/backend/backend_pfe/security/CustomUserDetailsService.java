package com.backend.backend_pfe.security;

import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user-specific data from the database for Spring Security.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This service only handles user lookup for authentication.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on the UserRepository abstraction (Spring Data JPA interface),
 *   not on a concrete DAO implementation.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by email address (used as the "username" in our system).
     *
     * @param email the user's email
     * @return a UserDetails adapter wrapping the domain User
     * @throws UsernameNotFoundException if no user with this email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable avec l'email : " + email));
        return new UserDetailsAdapter(user);
    }
}
