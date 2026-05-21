package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.Role;
import com.backend.backend_pfe.enums.StatutProjet;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;
    private final AnomalieService anomalieService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void importAffectations(MultipartFile file, Authentication authentication) throws IOException {
        // Pour la soutenance, on vide les anciennes données pour éviter les cumuls (600% etc.)
        affectationRepository.deleteAll();
        
        // Récupérer l'utilisateur actuel (celui qui importe) pour l'assigner comme chef de projet
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElse(null);

        System.out.println("DEBUG - ExcelImportService: Début ouverture workbook");
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        System.out.println("DEBUG - ExcelImportService: Workbook ouvert avec succès.");
        
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();
        
        if (rows.hasNext()) rows.next(); // Skip header

        while (rows.hasNext()) {
            Row currentRow = rows.next();
            try {
                String projectName = getCellValue(currentRow.getCell(0));
                String fullName = getCellValue(currentRow.getCell(1));
                String moisStr = getCellValue(currentRow.getCell(2));
                String anneeStr = getCellValue(currentRow.getCell(3));
                String joursStr = getCellValue(currentRow.getCell(4));
                String tauxStr = getCellValue(currentRow.getCell(5));
                String tjmStr = getCellValue(currentRow.getCell(6));
                String statutExcel = getCellValue(currentRow.getCell(7));

                if (projectName.isEmpty() || fullName.isEmpty()) continue;

                // 1. Gérer l'utilisateur (Recherche plus flexible)
                String targetEmail = fullName.toLowerCase().trim().replace(" ", ".") + "@soprabanking.com";
                if (!targetEmail.contains(".")) {
                     targetEmail = fullName.toLowerCase().trim() + "@soprabanking.com";
                }
                
                final String finalEmail = targetEmail;
                User user = userRepository.findByEmail(finalEmail)
                        .orElseGet(() -> {
                            User newUser = new User();
                            String[] parts = fullName.split(" ");
                            newUser.setNom(parts.length > 0 ? parts[0] : fullName);
                            newUser.setPrenom(parts.length > 1 ? parts[1] : "");
                            newUser.setEmail(finalEmail);
                            newUser.setPassword(passwordEncoder.encode("Staff2Staff2026!"));
                            newUser.setRole(Role.COLLABORATEUR);
                            newUser.setDisponible(true);
                            newUser.setTauxStaffing(0.0);
                            return userRepository.save(newUser);
                        });

                // 2. Gérer le projet
                Projet projet = projetRepository.findByNom(projectName)
                        .map(p -> {
                            if (p.getChefProjet() == null) {
                                p.setChefProjet(currentUser);
                                return projetRepository.save(p);
                            }
                            return p;
                        })
                        .orElseGet(() -> {
                            Projet newProjet = new Projet();
                            newProjet.setNom(projectName);
                            newProjet.setDescription("Importé via Excel");
                            newProjet.setStatut(StatutProjet.EN_COURS);
                            newProjet.setChefProjet(currentUser);
                            newProjet.setDateDebut(LocalDateTime.now().toLocalDate());
                            newProjet.setDateFin(LocalDateTime.now().plusMonths(6).toLocalDate());
                            return projetRepository.save(newProjet);
                        });

                // 3. Créer l'affectation
                Affectation affectation = new Affectation();
                affectation.setCollaborateur(user);
                affectation.setProjet(projet);
                
                try {
                    double tx = Double.parseDouble(tauxStr.replace("%", "").trim());
                    // Si le taux est < 1 (ex: 0.5), on le multiplie par 100
                    affectation.setTauxAffectation(tx < 1 ? tx * 100 : tx);
                } catch (Exception e) {
                    affectation.setTauxAffectation(100.0);
                }

                try {
                    affectation.setTjm(Double.parseDouble(tjmStr));
                    affectation.setNombreJours((int) Double.parseDouble(joursStr));
                } catch (Exception e) {
                    affectation.setTjm(0.0);
                    affectation.setNombreJours(0);
                }

                try {
                    int mois = (int) Double.parseDouble(moisStr);
                    int annee = (int) Double.parseDouble(anneeStr);
                    affectation.setDateDebut(LocalDateTime.of(annee, mois, 1, 0, 0).toLocalDate());
                    affectation.setDateFin(LocalDateTime.of(annee, mois, 1, 0, 0).plusMonths(1).minusDays(1).toLocalDate());
                } catch (Exception e) {
                    affectation.setDateDebut(LocalDateTime.now().toLocalDate());
                    affectation.setDateFin(LocalDateTime.now().plusMonths(1).toLocalDate());
                }

                affectation.setRoleDansProjet(statutExcel.isEmpty() ? "Consultant" : statutExcel);
                affectationRepository.save(affectation);

                anomalieService.detecterSurcharge(user.getId());
            } catch (Exception e) {
                System.out.println("ERROR - Ligne Excel: " + e.getMessage());
            }
        }
        workbook.close();
        System.out.println("DEBUG - Importation terminée avec succès");
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            default: return "";
        }
    }
}
