package com.backend.backend_pfe.service;

import com.backend.backend_pfe.DTO.response.CalendrierConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implémentation du service calendrier.
 * Utilise l'API Nager.Date (https://date.nager.at) pour les jours fériés.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendrierServiceImpl implements CalendrierService {

    private static final String NAGER_API = "https://date.nager.at/api/v3/PublicHolidays";

    // Mapping pays frontend → code ISO Nager.Date
    private static final Map<String, String> COUNTRY_CODES = Map.of(
            "ma", "MA",
            "fr", "FR",
            "be", "BE",
            "ch", "CH",
            "tn", "TN"
    );

    @Override
    public CalendrierConfigDTO getCalendrier(String pays, int annee) {
        String countryCode = COUNTRY_CODES.getOrDefault(pays.toLowerCase(), "MA");

        // 1. Récupérer les jours fériés depuis Nager.Date
        List<CalendrierConfigDTO.JourFerieDTO> joursFeries = fetchJoursFeries(countryCode, annee);

        // 2. Calculer les jours ouvrables par mois
        Set<LocalDate> feriesSet = joursFeries.stream()
                .filter(CalendrierConfigDTO.JourFerieDTO::isActif)
                .map(jf -> LocalDate.parse(jf.getDate()))
                .collect(Collectors.toSet());

        List<CalendrierConfigDTO.MoisOuvrableDTO> moisList = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(annee, m);
            int joursTotal = ym.lengthOfMonth();
            int weekends = 0;
            int feriesDansMois = 0;

            for (int d = 1; d <= joursTotal; d++) {
                LocalDate date = LocalDate.of(annee, m, d);
                DayOfWeek dow = date.getDayOfWeek();
                boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;

                if (isWeekend) {
                    weekends++;
                } else if (feriesSet.contains(date)) {
                    feriesDansMois++;
                }
            }

            int joursOuvrablesAuto = joursTotal - weekends - feriesDansMois;

            String label = capitalize(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH))
                    + " " + annee;

            moisList.add(CalendrierConfigDTO.MoisOuvrableDTO.builder()
                    .mois(m)
                    .label(label)
                    .joursTotal(joursTotal)
                    .weekends(weekends)
                    .joursFeries(feriesDansMois)
                    .joursOuvrablesAuto(joursOuvrablesAuto)
                    .joursOuvrablesManuel(null) // pas de modification manuelle par défaut
                    .valide(false)
                    .build());
        }

        return CalendrierConfigDTO.builder()
                .pays(pays)
                .annee(annee)
                .mois(moisList)
                .joursFeries(joursFeries)
                .build();
    }

    /**
     * Appelle l'API Nager.Date pour récupérer les jours fériés d'un pays/année.
     * Pour le Maroc, utilise une liste complète incluant les fêtes religieuses.
     * En cas d'erreur, retourne une liste vide (fallback gracieux).
     */
    private List<CalendrierConfigDTO.JourFerieDTO> fetchJoursFeries(String countryCode, int annee) {
        // Pour le Maroc : liste complète avec fêtes religieuses
        if ("MA".equals(countryCode)) {
            return getJoursFeriesMaroc(annee);
        }

        // Pour les autres pays : Nager.Date API
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = NAGER_API + "/" + annee + "/" + countryCode;

            NagerHoliday[] holidays = restTemplate.getForObject(url, NagerHoliday[].class);
            if (holidays == null) return List.of();

            return Arrays.stream(holidays)
                    .map(h -> CalendrierConfigDTO.JourFerieDTO.builder()
                            .date(h.date)
                            .nom(h.localName != null ? h.localName : h.name)
                            .actif(true)
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Impossible de récupérer les jours fériés depuis Nager.Date pour {}/{}: {}",
                    countryCode, annee, e.getMessage());
            return List.of();
        }
    }

    /**
     * Jours fériés marocains complets (fixes + religieux).
     * Les fêtes religieuses sont basées sur le calendrier hégirien et changent chaque année.
     */
    private List<CalendrierConfigDTO.JourFerieDTO> getJoursFeriesMaroc(int annee) {
        List<CalendrierConfigDTO.JourFerieDTO> feries = new ArrayList<>();

        // ═══ FÊTES FIXES (même date chaque année) ═══
        feries.add(ferie(annee + "-01-01", "Nouvel An"));
        feries.add(ferie(annee + "-01-11", "Manifeste de l'Indépendance"));
        feries.add(ferie(annee + "-05-01", "Fête du Travail"));
        feries.add(ferie(annee + "-07-30", "Fête du Trône"));
        feries.add(ferie(annee + "-08-14", "Journée Oued Ed-Dahab"));
        feries.add(ferie(annee + "-08-20", "Révolution du Roi et du Peuple"));
        feries.add(ferie(annee + "-08-21", "Fête de la Jeunesse"));
        feries.add(ferie(annee + "-11-06", "Marche Verte"));
        feries.add(ferie(annee + "-11-18", "Fête de l'Indépendance"));

        // ═══ FÊTES RELIGIEUSES (dates approximatives basées sur le calendrier hégirien) ═══
        switch (annee) {
            case 2025 -> {
                feries.add(ferie("2025-01-29", "Aïd Al Mawlid Annabawi (1er jour)"));
                feries.add(ferie("2025-01-30", "Aïd Al Mawlid Annabawi (2ème jour)"));
                feries.add(ferie("2025-03-30", "Aïd Al Fitr (1er jour)"));
                feries.add(ferie("2025-03-31", "Aïd Al Fitr (2ème jour)"));
                feries.add(ferie("2025-06-06", "Aïd Al Adha (1er jour)"));
                feries.add(ferie("2025-06-07", "Aïd Al Adha (2ème jour)"));
                feries.add(ferie("2025-06-27", "1er Moharram (Nouvel An Hégirien)"));
            }
            case 2026 -> {
                feries.add(ferie("2026-01-19", "Aïd Al Mawlid Annabawi (1er jour)"));
                feries.add(ferie("2026-01-20", "Aïd Al Mawlid Annabawi (2ème jour)"));
                feries.add(ferie("2026-03-20", "Aïd Al Fitr (1er jour)"));
                feries.add(ferie("2026-03-21", "Aïd Al Fitr (2ème jour)"));
                feries.add(ferie("2026-05-27", "Aïd Al Adha (1er jour)"));
                feries.add(ferie("2026-05-28", "Aïd Al Adha (2ème jour)"));
                feries.add(ferie("2026-06-17", "1er Moharram (Nouvel An Hégirien)"));
            }
            case 2027 -> {
                feries.add(ferie("2027-01-08", "Aïd Al Mawlid Annabawi (1er jour)"));
                feries.add(ferie("2027-01-09", "Aïd Al Mawlid Annabawi (2ème jour)"));
                feries.add(ferie("2027-03-10", "Aïd Al Fitr (1er jour)"));
                feries.add(ferie("2027-03-11", "Aïd Al Fitr (2ème jour)"));
                feries.add(ferie("2027-05-16", "Aïd Al Adha (1er jour)"));
                feries.add(ferie("2027-05-17", "Aïd Al Adha (2ème jour)"));
                feries.add(ferie("2027-06-06", "1er Moharram (Nouvel An Hégirien)"));
                feries.add(ferie("2027-12-28", "Aïd Al Mawlid Annabawi (1er jour)"));
                feries.add(ferie("2027-12-29", "Aïd Al Mawlid Annabawi (2ème jour)"));
            }
            case 2028 -> {
                feries.add(ferie("2028-02-27", "Aïd Al Fitr (1er jour)"));
                feries.add(ferie("2028-02-28", "Aïd Al Fitr (2ème jour)"));
                feries.add(ferie("2028-05-05", "Aïd Al Adha (1er jour)"));
                feries.add(ferie("2028-05-06", "Aïd Al Adha (2ème jour)"));
                feries.add(ferie("2028-05-26", "1er Moharram (Nouvel An Hégirien)"));
                feries.add(ferie("2028-12-16", "Aïd Al Mawlid Annabawi (1er jour)"));
                feries.add(ferie("2028-12-17", "Aïd Al Mawlid Annabawi (2ème jour)"));
            }
            default -> {
                // Pas de données religieuses pour cette année
                log.info("Pas de données de fêtes religieuses pour l'année {}", annee);
            }
        }

        // Trier par date
        feries.sort(Comparator.comparing(CalendrierConfigDTO.JourFerieDTO::getDate));
        return feries;
    }

    private CalendrierConfigDTO.JourFerieDTO ferie(String date, String nom) {
        return CalendrierConfigDTO.JourFerieDTO.builder()
                .date(date)
                .nom(nom)
                .actif(true)
                .build();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /** DTO interne pour mapper la réponse de Nager.Date */
    private static class NagerHoliday {
        public String date;
        public String localName;
        public String name;
        public String countryCode;
        public boolean fixed;
        public boolean global;
    }
}
