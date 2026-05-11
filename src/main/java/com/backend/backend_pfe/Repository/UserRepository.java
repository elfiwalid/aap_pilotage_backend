package com.backend.backend_pfe.Repository;

import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

}