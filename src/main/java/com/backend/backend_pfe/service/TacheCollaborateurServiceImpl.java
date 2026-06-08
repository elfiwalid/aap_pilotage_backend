package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.ImportTachesResponseDTO;
import com.backend.backend_pfe.DTO.response.TacheCollaborateurDTO;
import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.AffectationTacheCollaborateur;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.AffectationTacheCollaborateurRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.Repository.UserRepository;
import com.backend.backend_pfe.exception.BusinessValidationException;
import com.backend.backend_pfe.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TacheCollaborateurServiceImpl implements TacheCollaborateurService {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final AffectationRepository affectationRepository;
    private final AffectationTacheCollaborateurRepository tacheRepository;
    private final StaffingCalculService staffingCalculService;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls");

    private record TaskLine(
            int rowNumber,
            String matricule,
            String nom,
            String prenom,
            String tache,
            int nombreJours,
            LocalDate dateDebutV2,
            LocalDate dateFinV2) {
    }

    private record TaskGroup(
            User collaborateur,
            Affectation affectation,
            LocalDate dateDebut,
            LocalDate dateFin) {
    }

    @Override
    @Transactional
    public ImportTachesResponseDTO importerTaches(Long projetId, MultipartFile file, Authentication authentication) {
        User chef = resolveUser(authentication);
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        verifyOwnership(projet, chef);
        validateFile(file);

        List<TaskLine> lines = parseExcel(file);
        if (lines.isEmpty()) {
            throw new BusinessValidationException("Le fichier des taches ne contient aucune ligne exploitable");
        }

        List<Affectation> affectationsProjet = affectationRepository.findByProjet(projet).stream()
                .filter(a -> a.getCollaborateur() != null)
                .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                .toList();
        if (affectationsProjet.isEmpty()) {
            throw new BusinessValidationException(
                    "Aucune affectation V2 n'existe pour ce projet. Importez d'abord le fichier V2.");
        }

        Map<TaskGroup, List<TaskLine>> grouped = new LinkedHashMap<>();
        for (TaskLine line : lines) {
            User collaborateur = resolveCollaborateurFromV2(line, affectationsProjet);
            Affectation affectation = resolveAffectationForPeriod(line, collaborateur, affectationsProjet);
            TaskGroup group = new TaskGroup(collaborateur, affectation, line.dateDebutV2(), line.dateFinV2());
            grouped.computeIfAbsent(group, key -> new ArrayList<>()).add(line);
        }

        for (Map.Entry<TaskGroup, List<TaskLine>> entry : grouped.entrySet()) {
            TaskGroup group = entry.getKey();
            int joursDisponibles = staffingCalculService.countJoursOuvrables(group.dateDebut(), group.dateFin());
            int joursDemandes = entry.getValue().stream().mapToInt(TaskLine::nombreJours).sum();
            if (joursDemandes > joursDisponibles) {
                throw new BusinessValidationException(String.format(
                        "Les taches de %s depassent la capacite V2 (%d jours demandes pour %d jours ouvrables disponibles entre %s et %s).",
                        fullName(group.collaborateur()), joursDemandes, joursDisponibles,
                        group.dateDebut(), group.dateFin()));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<AffectationTacheCollaborateur> created = new ArrayList<>();
        for (Map.Entry<TaskGroup, List<TaskLine>> entry : grouped.entrySet()) {
            TaskGroup group = entry.getKey();
            tacheRepository.deleteByAffectationInAndDateTacheBetween(
                    List.of(group.affectation()), group.dateDebut(), group.dateFin());

            List<LocalDate> joursOuvrables = workingDays(group.dateDebut(), group.dateFin());
            int dayIndex = 0;
            int ordre = 1;
            for (TaskLine line : entry.getValue()) {
                for (int i = 0; i < line.nombreJours(); i++) {
                    LocalDate date = joursOuvrables.get(dayIndex++);
                    created.add(AffectationTacheCollaborateur.builder()
                            .projet(group.affectation().getProjet())
                            .collaborateur(group.collaborateur())
                            .affectation(group.affectation())
                            .tache(line.tache())
                            .dateTache(date)
                            .ordreJour(ordre++)
                            .dateDebutV2(group.dateDebut())
                            .dateFinV2(group.dateFin())
                            .dateImport(now)
                            .build());
                }
            }
        }

        List<AffectationTacheCollaborateur> saved = tacheRepository.saveAll(created);
        log.info("Import taches: {} lignes traitees, {} jours planifies pour projet {}",
                lines.size(), saved.size(), projet.getId());

        return ImportTachesResponseDTO.builder()
                .lignesTraitees(lines.size())
                .tachesPlanifiees(saved.size())
                .collaborateursConcernes(grouped.keySet().stream()
                        .map(g -> g.collaborateur().getId())
                        .collect(Collectors.toSet()).size())
                .taches(saved.stream().map(this::toDTO).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TacheCollaborateurDTO> getTachesCollaborateur(
            Authentication authentication, Integer annee, Integer mois) {
        User collaborateur = resolveUser(authentication);
        LocalDate debut;
        LocalDate fin;
        if (annee != null && mois != null) {
            YearMonth ym = YearMonth.of(annee, mois);
            debut = ym.atDay(1);
            fin = ym.atEndOfMonth();
        } else {
            debut = LocalDate.of(1970, 1, 1);
            fin = LocalDate.of(2999, 12, 31);
        }

        return tacheRepository
                .findByCollaborateurAndDateTacheBetweenOrderByDateTacheAscOrdreJourAsc(collaborateur, debut, fin)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TacheCollaborateurDTO> getTachesProjet(Long projetId, Authentication authentication) {
        User chef = resolveUser(authentication);
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        verifyOwnership(projet, chef);

        return tacheRepository.findByProjetOrderByDateTacheAscOrdreJourAsc(projet)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private List<TaskLine> parseExcel(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerRowIdx = findHeaderRow(sheet);
            if (headerRowIdx < 0) {
                throw new BusinessValidationException(
                        "Ligne d'en-tete introuvable. Colonnes attendues: nomCollaborateur, prenomCollaborateur, tache, nombreJours, dateDebutV2, dateFinV2.");
            }

            Map<String, Integer> columns = readColumns(sheet.getRow(headerRowIdx));
            requireColumns(columns, "tache", "nombrejours", "datedebutv2", "datefinv2");
            if (!columns.containsKey("matricule")
                    && (!columns.containsKey("nomcollaborateur") || !columns.containsKey("prenomcollaborateur"))) {
                throw new BusinessValidationException(
                        "Le fichier doit contenir soit matricule, soit nomCollaborateur et prenomCollaborateur.");
            }

            List<TaskLine> lines = new ArrayList<>();
            for (int i = headerRowIdx + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) continue;

                String matricule = getOptionalString(row, columns, "matricule");
                String nom = getOptionalString(row, columns, "nomcollaborateur");
                String prenom = getOptionalString(row, columns, "prenomcollaborateur");
                String tache = getRequiredString(row, columns, "tache", i + 1);
                int nombreJours = getRequiredPositiveInt(row, columns, "nombrejours", i + 1);
                LocalDate dateDebut = getRequiredDate(row, columns, "datedebutv2", i + 1);
                LocalDate dateFin = getRequiredDate(row, columns, "datefinv2", i + 1);

                if (dateFin.isBefore(dateDebut)) {
                    throw new BusinessValidationException("Ligne " + (i + 1)
                            + ": dateFinV2 doit etre posterieure ou egale a dateDebutV2.");
                }
                if ((matricule == null || matricule.isBlank())
                        && (nom == null || nom.isBlank() || prenom == null || prenom.isBlank())) {
                    throw new BusinessValidationException("Ligne " + (i + 1)
                            + ": renseignez matricule ou nomCollaborateur/prenomCollaborateur.");
                }

                lines.add(new TaskLine(i + 1, matricule, nom, prenom, tache,
                        nombreJours, dateDebut, dateFin));
            }
            return lines;
        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'import des taches: {}", e.getMessage(), e);
            throw new BusinessValidationException("Le fichier des taches est invalide ou illisible.");
        }
    }

    private User resolveCollaborateurFromV2(TaskLine line, List<Affectation> affectationsProjet) {
        if (line.matricule() != null && !line.matricule().isBlank()) {
            return affectationsProjet.stream()
                    .map(Affectation::getCollaborateur)
                    .filter(u -> line.matricule().equalsIgnoreCase(nullToEmpty(u.getMatricule())))
                    .findFirst()
                    .orElseThrow(() -> new BusinessValidationException("Ligne " + line.rowNumber()
                            + ": collaborateur matricule " + line.matricule() + " introuvable dans le V2 du projet."));
        }

        String expectedNom = normalize(line.nom());
        String expectedPrenom = normalize(line.prenom());
        return affectationsProjet.stream()
                .map(Affectation::getCollaborateur)
                .filter(u -> normalize(u.getNom()).equals(expectedNom)
                        && normalize(u.getPrenom()).equals(expectedPrenom))
                .findFirst()
                .orElseThrow(() -> new BusinessValidationException("Ligne " + line.rowNumber()
                        + ": collaborateur " + line.prenom() + " " + line.nom()
                        + " introuvable dans le V2 du projet."));
    }

    private Affectation resolveAffectationForPeriod(
            TaskLine line, User collaborateur, List<Affectation> affectationsProjet) {
        List<Affectation> candidates = affectationsProjet.stream()
                .filter(a -> a.getCollaborateur().getId().equals(collaborateur.getId()))
                .filter(a -> !line.dateDebutV2().isBefore(a.getDateDebut()))
                .filter(a -> !line.dateFinV2().isAfter(a.getDateFin()))
                .toList();

        if (candidates.isEmpty()) {
            throw new BusinessValidationException("Ligne " + line.rowNumber()
                    + ": la periode " + line.dateDebutV2() + " -> " + line.dateFinV2()
                    + " n'est pas couverte par le V2 de " + fullName(collaborateur) + ".");
        }

        return candidates.stream()
                .filter(a -> a.getDateDebut().equals(line.dateDebutV2())
                        && a.getDateFin().equals(line.dateFinV2()))
                .findFirst()
                .orElse(candidates.get(0));
    }

    private List<LocalDate> workingDays(LocalDate debut, LocalDate fin) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            if (staffingCalculService.countJoursOuvrables(current, current) == 1) {
                days.add(current);
            }
            current = current.plusDays(1);
        }
        return days;
    }

    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(15, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Map<String, Integer> columns = readColumns(row);
            if (columns.containsKey("tache") && columns.containsKey("nombrejours")) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> readColumns(Row row) {
        Map<String, Integer> columns = new HashMap<>();
        if (row == null) return columns;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String value = getCellStringValue(row.getCell(i));
            if (value != null && !value.isBlank()) {
                columns.put(normalizeHeader(value), i);
            }
        }
        return columns;
    }

    private void requireColumns(Map<String, Integer> columns, String... required) {
        for (String column : required) {
            if (!columns.containsKey(column)) {
                throw new BusinessValidationException("Colonne obligatoire manquante: " + column);
            }
        }
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String value = getCellStringValue(row.getCell(i));
            if (value != null && !value.isBlank()) return false;
        }
        return true;
    }

    private String getOptionalString(Row row, Map<String, Integer> columns, String key) {
        Integer idx = columns.get(key);
        return idx == null ? null : getCellStringValue(row.getCell(idx));
    }

    private String getRequiredString(Row row, Map<String, Integer> columns, String key, int rowNumber) {
        String value = getOptionalString(row, columns, key);
        if (value == null || value.isBlank()) {
            throw new BusinessValidationException("Ligne " + rowNumber + ": colonne " + key + " obligatoire.");
        }
        return value.trim();
    }

    private int getRequiredPositiveInt(Row row, Map<String, Integer> columns, String key, int rowNumber) {
        Cell cell = row.getCell(columns.get(key));
        double value = getCellNumericValue(cell);
        if (value <= 0 || value % 1 != 0) {
            throw new BusinessValidationException("Ligne " + rowNumber
                    + ": nombreJours doit etre un entier positif.");
        }
        return (int) value;
    }

    private LocalDate getRequiredDate(Row row, Map<String, Integer> columns, String key, int rowNumber) {
        LocalDate date = parseDateFromCell(row.getCell(columns.get(key)));
        if (date == null) {
            throw new BusinessValidationException("Ligne " + rowNumber
                    + ": date invalide pour la colonne " + key + ".");
        }
        return date;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                if (value == Math.rint(value)) yield String.valueOf((long) value);
                yield String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> null;
            default -> null;
        };
    }

    private double getCellNumericValue(Cell cell) {
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private LocalDate parseDateFromCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String value = getCellStringValue(cell);
            if (value == null || value.isBlank()) return null;
            if (value.contains("/")) {
                String[] parts = value.split("/");
                if (parts.length == 3) {
                    return LocalDate.of(Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                }
            }
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private TacheCollaborateurDTO toDTO(AffectationTacheCollaborateur tache) {
        User collaborateur = tache.getCollaborateur();
        Projet projet = tache.getProjet();
        return TacheCollaborateurDTO.builder()
                .id(tache.getId())
                .projetId(projet.getId())
                .projetNom(projet.getNom())
                .collaborateurId(collaborateur.getId())
                .collaborateurNomComplet(fullName(collaborateur))
                .matricule(collaborateur.getMatricule())
                .tache(tache.getTache())
                .dateTache(tache.getDateTache())
                .ordreJour(tache.getOrdreJour())
                .dateDebutV2(tache.getDateDebutV2())
                .dateFinV2(tache.getDateFinV2())
                .build();
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private void verifyOwnership(Projet projet, User user) {
        if (projet.getChefProjet() == null || !projet.getChefProjet().getId().equals(user.getId())) {
            throw new AccessDeniedException("Acces refuse");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BusinessValidationException("Le fichier des taches est vide");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessValidationException("La taille maximale autorisee est de 10 Mo");
        }
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessValidationException("Seuls les fichiers Excel (.xlsx, .xls) sont acceptes");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String normalizeHeader(String input) {
        return normalize(input).replace("_", "").replace("-", "").replace(" ", "");
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    private String nullToEmpty(String input) {
        return input == null ? "" : input;
    }

    private String fullName(User user) {
        return (nullToEmpty(user.getPrenom()) + " " + nullToEmpty(user.getNom())).trim();
    }
}
