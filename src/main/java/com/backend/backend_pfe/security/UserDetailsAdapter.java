package com.backend.backend_pfe.security;

import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter that wraps our domain {@link User} entity into Spring Security's
 * {@link UserDetails} contract.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   This class has one job: adapt a domain User to a UserDetails.
 *
 * SOLID — Liskov Substitution Principle (LSP):
 *   Any code expecting a UserDetails can use this adapter transparently.
 *
 * Design Pattern — Adapter:
 *   Converts the interface of the User entity into the UserDetails
 *   interface that Spring Security expects.
 */
@RequiredArgsConstructor
public class UserDetailsAdapter implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // We use email as the unique identifier
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Exposes the underlying domain User for downstream use.
     */
    public User getUser() {
        return user;
    }
}
