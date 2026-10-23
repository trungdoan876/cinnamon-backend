package com.cinnamon.controller;

import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {
        // Sanitize folder name
        String cleanFolder = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        if (cleanFolder.isEmpty()) cleanFolder = "general";

        String fileUrl = storageService.store(file, cleanFolder);
        return ResponseEntity.ok(ApiResponse.success("Tải file lên thành công", Map.of("url", fileUrl)));
    }
}
