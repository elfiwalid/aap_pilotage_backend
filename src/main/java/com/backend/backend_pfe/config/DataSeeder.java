package com.backend.backend_pfe.config;

import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with default users on application startup.
 * Only seeds the 5 core accounts (RM + 3 chefs + 1 collab).
 * Other collaborators are created dynamically when V2 Excel files are imported.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("Fatima Zahra", "Bennis",
                "fz.bennis@soprabanking.com", "Rm@Staff2026!",
                "MAT-RM-001", Role.RESOURCE_MANAGER, "Resource Manager");

        seedUser("Khalid", "Bennani",
                "khalid.bennani@soprabanking.com", "Pm@Staff2026!",
                "MAT-PM-002", Role.CHEF_PROJET, "Chef de Projet");

        seedUser("Youssef", "El Amrani",
                "youssef.elamrani@soprabanking.com", "Collab@Staff2026!",
                "MAT-COL-003", Role.COLLABORATEUR, "Collaborateur");

        seedUser("Chaimaa", "Kaddouri",
                "chaimaa.kaddouri@soprabanking.com", "Collab@Staff2026!",
                "MAT-COL-006", Role.COLLABORATEUR, "Collaborateur");

        seedUser("Sara", "Idrissi",
                "sara.idrissi@soprabanking.com", "Pm@Staff2026!",
                "MAT-PM-004", Role.CHEF_PROJET, "Chef de Projet");

        seedUser("Omar", "Tazi",
                "omar.tazi@soprabanking.com", "Pm@Staff2026!",
                "MAT-PM-005", Role.CHEF_PROJET, "Chef de Projet");
    }

    private void seedUser(String prenom, String nom, String email,
                          String rawPassword, String matricule,
                          Role role, String poste) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        User user = User.builder()
                .prenom(prenom)
                .nom(nom)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .matricule(matricule)
                .role(role)
                .poste(poste)
                .tauxStaffing(100.0)
                .disponible(true)
                .build();

        userRepository.save(user);
        log.info("✔ Utilisateur créé : {} ({})", email, role);
    }
}
