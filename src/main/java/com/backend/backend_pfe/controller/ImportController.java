package com.backend.backend_pfe.controller;

import com.backend.backend_pfe.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import java.io.IOException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService excelImportService;

    @PostMapping("/excel")
    public ResponseEntity<String> importExcel(@RequestParam("file") MultipartFile file, Authentication authentication) {
        System.out.println("#########################################################");
        System.out.println("!!! REQUETE POST /api/import/excel RECUE !!!");
        System.out.println("Fichier: " + file.getOriginalFilename());
        System.out.println("#########################################################");
        try {
            excelImportService.importAffectations(file, authentication);
            return ResponseEntity.ok("Importation réussie !");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }
}
