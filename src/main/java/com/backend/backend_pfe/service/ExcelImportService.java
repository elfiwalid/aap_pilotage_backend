package com.backend.backend_pfe.service;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ExcelImportService {
    void importAffectations(MultipartFile file, Authentication authentication) throws IOException;
}
