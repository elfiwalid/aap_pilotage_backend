package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.PrevisionResponseDTO;
import com.backend.backend_pfe.DTO.response.PrevisionStatsDTO;
import com.backend.backend_pfe.Entity.Prevision;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AffectationTacheCollaborateurRepository;
import com.backend.backend_pfe.Repository.AnomalieV2Repository;
import com.backend.backend_pfe.Repository.PrevisionRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.SimulationWhatIfRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.enums.TypePrevision;
import com.backend.backend_pfe.enums.TypeNotification;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.AnomalieV2;
import com.backend.backend_pfe.enums.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Implementation of PrevisionService.
 *
 * SOLID — Single Responsibility Principle (SRP):
 *   Contains exclusively business logic (validation, archiving, parsing).
 *   Delegates data access to Repository interfaces.
 *
 * SOLID — Dependency Inversion Principle (DIP):
 *   Depends on Repository interfaces, not concrete implementations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrevisionServiceImpl implements PrevisionService {

    private final PrevisionRepository previsionRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final AffectationRepository affectationRepository;
    private final AnomalieDetectionService anomalieDetectionService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AnomalieDetectionV2Service anomalieDetectionV2Service;
    private final AffectationTacheCollaborateurRepository tacheCollaborateurRepository;
    private final AnomalieV2Repository anomalieV2Repository;
    private final SimulationWhatIfRepository simulationWhatIfRepository;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 Mo
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls");

    // --- Record interne pour le résultat du parsing Excel ---
    private record ExcelParseResult(LocalDate periodeDebut, LocalDate periodeFin) {}

    @Override
    @Transactional
    public PrevisionResponseDTO importerPrevision(Long projetId, MultipartFile file,
            TypePrevision typePrevision, LocalDate periodeDebut,
            LocalDate periodeFin, Authentication authentication) {
        // 1. Résoudre l'utilisateur authentifié
        User user = resolveUser(authentication);

        // 2. Résoudre le projet et vérifier l'ownership
        Projet projet = resolveProjetWithOwnershipCheck(projetId, user);

        // 3. Valider le fichier (extension, taille, contenu vide)
        validateFile(file);

        // 4. Valider les dates de période fournies par l'utilisateur
        if (periodeDebut == null || periodeFin == null) {
            throw new BusinessValidationException(
                    "Les dates de période sont obligatoires");
        }
        if (!periodeFin.isAfter(periodeDebut)) {
            throw new BusinessValidationException(
                    "La période de fin doit être postérieure à la période de début");
        }

        // 5. Archiver les anciennes prévisions actives du même type
        archivePreviousActive(projet, typePrevision);

        // 6. Créer et persister la nouvelle prévision
        try {
            Prevision prevision = Prevision.builder()
                    .nomFichier(truncateFileName(file.getOriginalFilename()))
                    .typePrevision(typePrevision)
                    .periodeDebut(periodeDebut)
                    .periodeFin(periodeFin)
                    .dateImport(LocalDateTime.now())
                    .active(true)
                    .importePar(user)
                    .projet(projet)
                    .fichierData(file.getBytes())
                    .build();

            Prevision saved = previsionRepository.save(prevision);

            // 7. Parser le fichier Excel et créer/mettre à jour les affectations
            try {
                parseAndCreateAffectations(file.getBytes(), projet, periodeDebut, periodeFin);
            } catch (Exception e) {
                log.error("Parsing des affectations échoué pour projet {}: {}",
                        projetId, e.getMessage());
                // L'import continue même si le parsing échoue
            }

            // 8. Déclencher la détection automatique V2 après l'import
            // On flush les affectations pour les rendre visibles à la détection
            // et on enregistre les mois à recalculer pour la détection
            try {
                affectationRepository.flush();
                int moisDetection = periodeDebut.getMonthValue();
                int anneeDetection = periodeDebut.getYear();
                List<AnomalieV2> anomaliesMois = anomalieDetectionV2Service
                        .detecterAnomalies(anneeDetection, moisDetection, "ma");
                notifierAnomaliesV2Import(projet, anomaliesMois, anneeDetection, moisDetection);
                log.info("Détection V2 lancée automatiquement pour {}/{}", moisDetection, anneeDetection);
                // Si la période fin est dans un autre mois, recalculer ce mois aussi
                if (periodeFin.getMonthValue() != moisDetection || periodeFin.getYear() != anneeDetection) {
                    List<AnomalieV2> anomaliesFin = anomalieDetectionV2Service
                            .detecterAnomalies(periodeFin.getYear(), periodeFin.getMonthValue(), "ma");
                    notifierAnomaliesV2Import(projet, anomaliesFin, periodeFin.getYear(), periodeFin.getMonthValue());
                    log.info("Détection V2 lancée aussi pour {}/{}", periodeFin.getMonthValue(), periodeFin.getYear());
                }
            } catch (Exception e) {
                log.error("Détection V2 échouée pour projet {}: {}", projetId, e.getMessage());
            }

            return mapToResponseDTO(saved);
        } catch (IOException e) {
            throw new BusinessValidationException("Erreur lors de la lecture du fichier");
        }
    }

    @Override
    public List<PrevisionResponseDTO> getHistorique(Long projetId, Authentication authentication) {
        User user = resolveUser(authentication);
        Projet projet = resolveProjetWithOwnershipCheck(projetId, user);

        List<Prevision> previsions = previsionRepository
                .findByProjetOrderByDateImportDesc(projet);

        return previsions.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PrevisionResponseDTO> getPrevisionActive(Long projetId, Authentication authentication) {
        User user = resolveUser(authentication);
        Projet projet = resolveProjetWithOwnershipCheck(projetId, user);

        return previsionRepository.findByProjetAndActiveTrue(projet).stream()
                .findFirst()
                .map(this::mapToResponseDTO);
    }

    @Override
    public ResponseEntity<byte[]> telechargerPrevision(Long previsionId, Authentication authentication) {
        User user = resolveUser(authentication);
        Prevision prevision = previsionRepository.findById(previsionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prévision introuvable"));

        verifyOwnership(prevision.getProjet(), user);

        if (prevision.getFichierData() == null || prevision.getFichierData().length == 0) {
            throw new ResourceNotFoundException("Fichier indisponible");
        }

        String contentType = determineContentType(prevision.getNomFichier());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + prevision.getNomFichier() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(prevision.getFichierData());
    }

    @Override
    public PrevisionStatsDTO getStatistiques(Long previsionId, Authentication authentication) {
        User user = resolveUser(authentication);
        Prevision prevision = previsionRepository.findById(previsionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prévision introuvable"));

        verifyOwnership(prevision.getProjet(), user);

        // Calculer le nombre de mois couverts
        int nombreMois = (int) ChronoUnit.MONTHS.between(
                prevision.getPeriodeDebut(), prevision.getPeriodeFin()) + 1;

        // Compter les collaborateurs distincts avec affectations chevauchantes
        int nombreCollaborateurs = countDistinctCollaborateurs(
                prevision.getProjet(), prevision.getPeriodeDebut(),
                prevision.getPeriodeFin());

        return PrevisionStatsDTO.builder()
                .nombreCollaborateurs(nombreCollaborateurs)
                .nombreMois(nombreMois)
                .typePrevision(prevision.getTypePrevision())
                .dateImport(prevision.getDateImport())
                .build();
    }

    @Override
    @Transactional
    public void supprimerPrevision(Long previsionId, Authentication authentication) {
        User user = resolveUser(authentication);
        Prevision prevision = previsionRepository.findById(previsionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prévision introuvable"));
        Projet projet = prevision.getProjet();
        verifyOwnership(projet, user);

        if (Boolean.TRUE.equals(prevision.getActive())) {
            List<Affectation> affectationsPeriode = affectationRepository.findByProjet(projet).stream()
                    .filter(a -> periodsOverlap(a.getDateDebut(), a.getDateFin(),
                            prevision.getPeriodeDebut(), prevision.getPeriodeFin()))
                    .toList();

            if (!affectationsPeriode.isEmpty()) {
                tacheCollaborateurRepository.deleteByAffectationIn(affectationsPeriode);
                affectationRepository.deleteAll(affectationsPeriode);
            }

            List<AnomalieV2> anomalies = findAnomaliesLieesPrevision(prevision);
            List<Long> anomalieIds = anomalies.stream().map(AnomalieV2::getId).toList();
            if (!anomalieIds.isEmpty()) {
                simulationWhatIfRepository.nullifyAnomalieIn(anomalieIds);
                anomalieV2Repository.deleteAll(anomalies);
            }
        }

        previsionRepository.delete(prevision);
    }

    private void notifierAnomaliesV2Import(
            Projet projet, List<AnomalieV2> anomalies, int annee, int mois) {
        if (projet == null || projet.getChefProjet() == null || anomalies == null || anomalies.isEmpty()) {
            return;
        }

        String nomProjet = projet.getNom() == null ? "" : projet.getNom().toLowerCase(Locale.ROOT);
        long totalProjet = anomalies.stream()
                .filter(a -> a.getProjetsConcernes() != null
                        && a.getProjetsConcernes().toLowerCase(Locale.ROOT).contains(nomProjet))
                .count();
        if (totalProjet == 0) {
            return;
        }

        String periode = String.format("%02d/%d", mois, annee);
        notificationService.creerNotification(
                projet.getChefProjet(),
                null,
                TypeNotification.ANOMALIE,
                "Anomalies V2 générées — " + projet.getNom(),
                String.format("%d anomalie(s) V2 ont été générée(s) pour le projet « %s » sur la période %s.",
                        totalProjet, projet.getNom(), periode),
                null);
    }

    // --- Méthodes privées utilitaires ---

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable"));
    }

    private Projet resolveProjetWithOwnershipCheck(Long projetId, User user) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projet introuvable"));
        verifyOwnership(projet, user);
        return projet;
    }

    private void verifyOwnership(Projet projet, User user) {
        if (!projet.getChefProjet().getId().equals(user.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty() || file.getSize() == 0) {
            throw new BusinessValidationException("Le fichier est vide");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessValidationException(
                    "La taille maximale autorisée est de 10 Mo");
        }
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessValidationException(
                    "Seuls les fichiers Excel (.xlsx, .xls) sont acceptés");
        }
    }

    private void archivePreviousActive(Projet projet, TypePrevision typePrevision) {
        List<Prevision> actives = previsionRepository
                .findByProjetAndTypePrevisionAndActiveTrue(projet, typePrevision);
        for (Prevision p : actives) {
            p.setActive(false);
        }
        if (!actives.isEmpty()) {
            previsionRepository.saveAll(actives);
        }
    }

    private PrevisionResponseDTO mapToResponseDTO(Prevision prevision) {
        User user = prevision.getImportePar();
        Projet projet = prevision.getProjet();

        return PrevisionResponseDTO.builder()
                .id(prevision.getId())
                .nomFichier(prevision.getNomFichier())
                .typePrevision(prevision.getTypePrevision())
                .periodeDebut(prevision.getPeriodeDebut())
                .periodeFin(prevision.getPeriodeFin())
                .dateImport(prevision.getDateImport())
                .active(prevision.getActive())
                .importeParNomComplet(user.getPrenom() + " " + user.getNom())
                .projetId(projet.getId())
                .projetNom(projet.getNom())
                .build();
    }

    /**
     * Parse le fichier Excel V2 et crée automatiquement :
     * - Les comptes collaborateurs s'ils n'existent pas (prenom.nom@soprabanking.com)
     * - Les affectations sur le projet pour la période donnée
     *
     * Format attendu du fichier Excel V2 :
     * - Lignes 0-2 : en-têtes/titre (ignorées)
     * - Ligne 3 : noms des colonnes
     * - Lignes 4+ : données collaborateurs
     * - Colonne 0 (A) : N° Employé (matricule)
     * - Colonne 1 (B) : Collaborateur (format "NOM PRENOM")
     * - Colonne 7 (H) : Nb_Projets
     * - Colonne 12 (M) : Jours travaillés dans le mois
     */
    private void parseAndCreateAffectations(byte[] fileData, Projet projet,
            LocalDate periodeDebut, LocalDate periodeFin) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileData))) {
            Sheet sheet = workbook.getSheetAt(0);

            // Déterminer la ligne d'en-tête (chercher "Collaborateur" ou "N° Employé")
            int headerRowIdx = findHeaderRow(sheet);
            if (headerRowIdx < 0) {
                log.warn("Impossible de trouver la ligne d'en-tête dans le fichier Excel");
                return;
            }

            // Supprimer les anciennes affectations de ce projet pour cette période
            // (pour éviter les doublons lors de ré-imports)
            List<Affectation> existantes = affectationRepository.findByProjet(projet);
            List<Affectation> aSupprimer = existantes.stream()
                    .filter(a -> periodsOverlap(a.getDateDebut(), a.getDateFin(), periodeDebut, periodeFin))
                    .toList();
            if (!aSupprimer.isEmpty()) {
                affectationRepository.deleteAll(aSupprimer);
                log.info("Supprimé {} anciennes affectations pour le projet {} sur la période {}-{}",
                        aSupprimer.size(), projet.getNom(), periodeDebut, periodeFin);
            }

            // Parser chaque ligne de données
            int created = 0;
            for (int i = headerRowIdx + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Colonne 0 : matricule
                String matricule = getCellStringValue(row.getCell(0));
                if (matricule == null || matricule.isBlank()) continue;

                // Colonne 1 : nom complet (format "NOM PRENOM" ou "NOM PRENOM1 PRENOM2")
                String nomComplet = getCellStringValue(row.getCell(1));
                if (nomComplet == null || nomComplet.isBlank()) continue;

                // Colonnes 10-11 : Date_Début et Date_Fin individuelles du collaborateur
                LocalDate dateDebutCollab = parseDateFromCell(row.getCell(10));
                LocalDate dateFinCollab = parseDateFromCell(row.getCell(11));
                // Fallback : si les dates ne sont pas parsables, utiliser la période globale
                if (dateDebutCollab == null) dateDebutCollab = periodeDebut;
                if (dateFinCollab == null) dateFinCollab = periodeFin;

                // Le taux d'affectation est 100% par défaut pour chaque collaborateur
                // listé dans le fichier de prévision. C'est le croisement entre
                // plusieurs projets qui génère les surcharges (>100%) et conflits.
                double tauxAffectation = 100.0;

                // Trouver ou créer le collaborateur
                User collaborateur = findOrCreateCollaborateur(matricule, nomComplet);

                // Créer l'affectation avec les dates individuelles du collaborateur
                Affectation affectation = Affectation.builder()
                        .collaborateur(collaborateur)
                        .projet(projet)
                        .tauxAffectation(tauxAffectation)
                        .dateDebut(dateDebutCollab)
                        .dateFin(dateFinCollab)
                        .roleDansProjet("Collaborateur")
                        .build();
                affectationRepository.save(affectation);
                created++;

                // Notifier le collaborateur de sa nouvelle affectation
                notificationService.notifierAffectation(collaborateur, projet, tauxAffectation);
            }

            log.info("Import Excel : {} affectations créées pour le projet '{}' (période {}-{})",
                    created, projet.getNom(), periodeDebut, periodeFin);

        } catch (Exception e) {
            log.error("Erreur lors du parsing Excel pour créer les affectations: {}", e.getMessage(), e);
        }
    }

    /**
     * Cherche la ligne d'en-tête dans le fichier Excel (contenant "Collaborateur" ou "N° Employé").
     */
    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (int j = 0; j < Math.min(5, row.getLastCellNum()); j++) {
                String val = getCellStringValue(row.getCell(j));
                if (val != null && (val.contains("Collaborateur") || val.contains("N° Employé")
                        || val.contains("Employé") || val.contains("Matricule"))) {
                    return i;
                }
            }
        }
        // Fallback : supposer que la ligne 3 est l'en-tête (format standard V2)
        return 3;
    }

    /**
     * Trouve un collaborateur par matricule ou email, ou le crée s'il n'existe pas.
     * Email généré : prenom.nom@soprabanking.com (tout en minuscule, sans accents)
     * Mot de passe par défaut : Collab@Staff2026!
     */
    private User findOrCreateCollaborateur(String matricule, String nomComplet) {
        // 1. Chercher par matricule
        Optional<User> byMatricule = userRepository.findByMatricule(matricule);
        if (byMatricule.isPresent()) {
            return byMatricule.get();
        }

        // 2. Parser le nom complet (format "NOM PRENOM" ou "NOM PRENOM1 PRENOM2")
        String[] parts = nomComplet.trim().split("\\s+");
        String nom;
        String prenom;
        if (parts.length >= 2) {
            nom = capitalize(parts[0]);
            prenom = capitalize(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)));
        } else {
            nom = capitalize(parts[0]);
            prenom = "Inconnu";
        }

        // 3. Générer l'email : prenom.nom@soprabanking.com
        String email = generateEmail(prenom, nom);

        // 4. Chercher par email
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            // Mettre à jour le matricule si manquant
            User existing = byEmail.get();
            if (existing.getMatricule() == null || existing.getMatricule().isBlank()) {
                existing.setMatricule(matricule);
                userRepository.save(existing);
            }
            return existing;
        }

        // 5. Créer le collaborateur
        User newUser = User.builder()
                .prenom(prenom)
                .nom(nom)
                .email(email)
                .password(passwordEncoder.encode("Collab@Staff2026!"))
                .matricule(matricule)
                .role(Role.COLLABORATEUR)
                .poste("Collaborateur")
                .tauxStaffing(100.0)
                .disponible(true)
                .build();

        User saved = userRepository.save(newUser);
        log.info("✔ Collaborateur créé automatiquement : {} {} ({}) - matricule {}",
                prenom, nom, email, matricule);
        return saved;
    }

    /**
     * Génère un email au format prenom.nom@soprabanking.com
     * Gère les prénoms composés et supprime les accents.
     */
    private String generateEmail(String prenom, String nom) {
        String cleanPrenom = removeAccents(prenom.trim().toLowerCase().replace(" ", "."));
        String cleanNom = removeAccents(nom.trim().toLowerCase().replace(" ", "."));
        return cleanPrenom + "." + cleanNom + "@soprabanking.com";
    }

    /**
     * Supprime les accents d'une chaîne.
     */
    private String removeAccents(String input) {
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    /**
     * Met en majuscule la première lettre de chaque mot.
     */
    private String capitalize(String input) {
        if (input == null || input.isBlank()) return input;
        String lower = input.toLowerCase();
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : lower.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Calcule le nombre approximatif de jours ouvrés dans une période.
     */
    private long calculateJoursOuvres(LocalDate debut, LocalDate fin) {
        long totalDays = ChronoUnit.DAYS.between(debut, fin) + 1;
        // Approximation : 5/7 des jours sont ouvrés
        return Math.round(totalDays * 5.0 / 7.0);
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    /**
     * Parse une date depuis une cellule Excel.
     * Supporte les formats : date numérique Excel, "dd/MM/yyyy", "yyyy-MM-dd"
     */
    private LocalDate parseDateFromCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) return null;
                // Try dd/MM/yyyy format
                if (val.contains("/")) {
                    String[] parts = val.split("/");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        return LocalDate.of(year, month, day);
                    }
                }
                // Try yyyy-MM-dd format
                if (val.contains("-") && val.length() == 10) {
                    return LocalDate.parse(val);
                }
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return null;
    }

    private double getCellNumericValue(Cell cell) {
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().replace(",", ".").trim());
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private ExcelParseResult parseExcelFile(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            Row secondRow = sheet.getRow(1);

            if (headerRow == null || secondRow == null) {
                throw new BusinessValidationException(
                        "Impossible d'extraire les dates de période du fichier");
            }

            LocalDate periodeDebut = extractDateFromCell(headerRow.getCell(1));
            LocalDate periodeFin = extractDateFromCell(secondRow.getCell(1));

            if (periodeDebut == null || periodeFin == null) {
                throw new BusinessValidationException(
                        "Impossible d'extraire les dates de période du fichier");
            }
            if (!periodeFin.isAfter(periodeDebut)) {
                throw new BusinessValidationException(
                        "La période de fin doit être postérieure à la période de début");
            }

            return new ExcelParseResult(periodeDebut, periodeFin);
        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessValidationException(
                    "Le contenu du fichier est invalide ou corrompu");
        }
    }

    private LocalDate extractDateFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return LocalDate.parse(cell.getStringCellValue());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String truncateFileName(String filename) {
        if (filename == null) {
            return "unknown.xlsx";
        }
        return filename.length() > 255 ? filename.substring(0, 255) : filename;
    }

    private String determineContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return switch (extension) {
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            default -> "application/octet-stream";
        };
    }

    private int countDistinctCollaborateurs(Projet projet, LocalDate debut, LocalDate fin) {
        List<Affectation> affectations = affectationRepository.findByProjet(projet);
        return (int) affectations.stream()
                .filter(a -> periodsOverlap(a.getDateDebut(), a.getDateFin(), debut, fin))
                .map(a -> a.getCollaborateur().getId())
                .distinct()
                .count();
    }

    private List<AnomalieV2> findAnomaliesLieesPrevision(Prevision prevision) {
        Projet projet = prevision.getProjet();
        String nomProjet = projet.getNom() == null ? "" : projet.getNom().toLowerCase(Locale.ROOT);
        List<AnomalieV2> result = new ArrayList<>();

        YearMonth current = YearMonth.from(prevision.getPeriodeDebut());
        YearMonth end = YearMonth.from(prevision.getPeriodeFin());
        while (!current.isAfter(end)) {
            result.addAll(anomalieV2Repository
                    .findByAnneeAndMoisOrderByDateDetectionDesc(current.getYear(), current.getMonthValue())
                    .stream()
                    .filter(anomalie -> anomalie.getProjetsConcernes() != null
                            && anomalie.getProjetsConcernes().toLowerCase(Locale.ROOT).contains(nomProjet))
                    .toList());
            current = current.plusMonths(1);
        }

        return result.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(AnomalieV2::getId, a -> a, (a, b) -> a, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())));
    }

    private boolean periodsOverlap(LocalDate aDebut, LocalDate aFin,
            LocalDate bDebut, LocalDate bFin) {
        if (aDebut == null || aFin == null || bDebut == null || bFin == null) {
            return false;
        }
        return !aFin.isBefore(bDebut) && !aDebut.isAfter(bFin);
    }
}
