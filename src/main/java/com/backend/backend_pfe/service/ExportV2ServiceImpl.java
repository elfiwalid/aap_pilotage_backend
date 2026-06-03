package com.backend.backend_pfe.service;

import com.backend.backend_pfe.Entity.Affectation;
import com.backend.backend_pfe.Entity.Projet;
import com.backend.backend_pfe.Entity.User;
import com.backend.backend_pfe.Repository.AffectationRepository;
import com.backend.backend_pfe.Repository.ProjetRepository;
import com.backend.backend_pfe.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère un fichier Excel V2 consolidé avec mise en forme professionnelle.
 * Chaque projet a sa propre couleur et section dans le fichier.
 */
@Service
@RequiredArgsConstructor
public class ExportV2ServiceImpl implements ExportV2Service {

    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;

    // Palette de couleurs pour les projets (hex sans #)
    private static final String[] PROJECT_COLORS = {
            "7B2CBF", "2D9CDB", "059669", "F59E0B",
            "E600A9", "8B5CF6", "EF4444", "0EA5E9",
            "10B981", "F97316", "6366F1", "EC4899"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public byte[] exporterV2Consolide(List<Long> projetIds) {
        List<Projet> projets = projetRepository.findAllById(projetIds);
        if (projets.isEmpty()) {
            throw new BusinessValidationException("Aucun projet sélectionné");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("V2 Consolidé");

            // Styles
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(hexToBytes("1F4E79"), null));

            XSSFFont subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setFontHeightInPoints((short) 10);

            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 10);

            // Title style
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            // Subtitle style
            XSSFCellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setFont(subtitleFont);

            int rowIdx = 0;

            // ═══ TITRE GLOBAL ═══
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("V2 Consolidé — Staffing Multi-Projets");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            Row infoRow = sheet.createRow(rowIdx++);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(String.format("Généré le %s  |  %d projet(s) sélectionné(s)  |  Export Resource Manager",
                    LocalDate.now().format(DATE_FMT), projets.size()));
            infoCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            rowIdx++; // ligne vide

            // ═══ POUR CHAQUE PROJET ═══
            for (int pIdx = 0; pIdx < projets.size(); pIdx++) {
                Projet projet = projets.get(pIdx);
                String colorHex = PROJECT_COLORS[pIdx % PROJECT_COLORS.length];
                List<Affectation> affectations = affectationRepository.findByProjet(projet);
                User chef = projet.getChefProjet();

                // Style header pour ce projet
                XSSFCellStyle projectHeaderStyle = workbook.createCellStyle();
                projectHeaderStyle.setFont(headerFont);
                projectHeaderStyle.setFillForegroundColor(new XSSFColor(hexToBytes(colorHex), null));
                projectHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                projectHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
                projectHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                projectHeaderStyle.setBorderBottom(BorderStyle.THIN);
                projectHeaderStyle.setBorderTop(BorderStyle.THIN);
                projectHeaderStyle.setBorderLeft(BorderStyle.THIN);
                projectHeaderStyle.setBorderRight(BorderStyle.THIN);

                // Style données pour ce projet (fond clair)
                XSSFCellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);
                dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                // Style projet titre
                XSSFCellStyle projectTitleStyle = workbook.createCellStyle();
                projectTitleStyle.setFont(boldFont);
                XSSFColor lightColor = new XSSFColor(hexToBytes(lightenColor(colorHex)), null);
                projectTitleStyle.setFillForegroundColor(lightColor);
                projectTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                projectTitleStyle.setBorderBottom(BorderStyle.THIN);

                // ─── Titre du projet ───
                Row projectRow = sheet.createRow(rowIdx++);
                Cell pCell = projectRow.createCell(0);
                pCell.setCellValue(String.format("📁 %s", projet.getNom()));
                pCell.setCellStyle(projectTitleStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 8));

                // Info projet
                Row metaRow = sheet.createRow(rowIdx++);
                metaRow.createCell(0).setCellValue("Chef de projet:");
                metaRow.createCell(1).setCellValue(chef != null ? chef.getPrenom() + " " + chef.getNom() : "N/A");
                metaRow.createCell(3).setCellValue("Statut:");
                metaRow.createCell(4).setCellValue(projet.getStatut() != null ? projet.getStatut().name() : "N/A");
                metaRow.createCell(6).setCellValue("Période:");
                metaRow.createCell(7).setCellValue(
                        (projet.getDateDebut() != null ? projet.getDateDebut().format(DATE_FMT) : "?")
                                + " → " +
                                (projet.getDateFin() != null ? projet.getDateFin().format(DATE_FMT) : "?"));

                rowIdx++; // espace

                // ─── En-têtes colonnes ───
                String[] headers = {"N° Employé", "Collaborateur", "Email", "Rôle", "Taux (%)",
                        "Date Début", "Date Fin", "Jours Ouvrés", "Charge Prévue"};
                Row headerRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(projectHeaderStyle);
                }

                // ─── Données collaborateurs ───
                if (affectations.isEmpty()) {
                    Row emptyRow = sheet.createRow(rowIdx++);
                    emptyRow.createCell(0).setCellValue("Aucun collaborateur affecté");
                } else {
                    for (int aIdx = 0; aIdx < affectations.size(); aIdx++) {
                        Affectation aff = affectations.get(aIdx);
                        User collab = aff.getCollaborateur();
                        Row dataRow = sheet.createRow(rowIdx++);

                        Cell c0 = dataRow.createCell(0); c0.setCellValue(collab.getMatricule() != null ? collab.getMatricule() : String.valueOf(aIdx + 1)); c0.setCellStyle(dataStyle);
                        Cell c1 = dataRow.createCell(1); c1.setCellValue(collab.getPrenom() + " " + collab.getNom()); c1.setCellStyle(dataStyle);
                        Cell c2 = dataRow.createCell(2); c2.setCellValue(collab.getEmail()); c2.setCellStyle(dataStyle);
                        Cell c3 = dataRow.createCell(3); c3.setCellValue(aff.getRoleDansProjet() != null ? aff.getRoleDansProjet() : "Collaborateur"); c3.setCellStyle(dataStyle);
                        Cell c4 = dataRow.createCell(4); c4.setCellValue(aff.getTauxAffectation() != null ? aff.getTauxAffectation() : 0); c4.setCellStyle(dataStyle);
                        Cell c5 = dataRow.createCell(5); c5.setCellValue(aff.getDateDebut() != null ? aff.getDateDebut().format(DATE_FMT) : ""); c5.setCellStyle(dataStyle);
                        Cell c6 = dataRow.createCell(6); c6.setCellValue(aff.getDateFin() != null ? aff.getDateFin().format(DATE_FMT) : ""); c6.setCellStyle(dataStyle);

                        // Jours ouvrés approximatifs
                        long joursOuvres = 0;
                        if (aff.getDateDebut() != null && aff.getDateFin() != null) {
                            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(aff.getDateDebut(), aff.getDateFin()) + 1;
                            joursOuvres = Math.round(totalDays * 5.0 / 7.0);
                        }
                        Cell c7 = dataRow.createCell(7); c7.setCellValue(joursOuvres); c7.setCellStyle(dataStyle);

                        // Charge prévue (jours * taux / 100)
                        double charge = joursOuvres * (aff.getTauxAffectation() != null ? aff.getTauxAffectation() / 100.0 : 0);
                        Cell c8 = dataRow.createCell(8); c8.setCellValue(Math.round(charge * 10.0) / 10.0); c8.setCellStyle(dataStyle);
                    }
                }

                // Ligne de total
                Row totalRow = sheet.createRow(rowIdx++);
                totalRow.createCell(0);
                Cell totalLabel = totalRow.createCell(3);
                totalLabel.setCellValue("TOTAL");
                totalLabel.setCellStyle(projectTitleStyle);
                Cell totalTaux = totalRow.createCell(4);
                double sumTaux = affectations.stream()
                        .mapToDouble(a -> a.getTauxAffectation() != null ? a.getTauxAffectation() : 0)
                        .sum();
                totalTaux.setCellValue(sumTaux + "%");
                totalTaux.setCellStyle(projectTitleStyle);
                Cell totalCollabs = totalRow.createCell(1);
                totalCollabs.setCellValue(affectations.size() + " collaborateur(s)");
                totalCollabs.setCellStyle(projectTitleStyle);

                rowIdx += 2; // espace entre projets
            }

            // Auto-size columns
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new BusinessValidationException("Erreur lors de la génération du fichier Excel: " + e.getMessage());
        }
    }

    private byte[] hexToBytes(String hex) {
        return new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    /** Éclaircit une couleur hex pour le fond des titres de projet */
    private String lightenColor(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        r = Math.min(255, r + (255 - r) * 3 / 4);
        g = Math.min(255, g + (255 - g) * 3 / 4);
        b = Math.min(255, b + (255 - b) * 3 / 4);
        return String.format("%02X%02X%02X", r, g, b);
    }
}
