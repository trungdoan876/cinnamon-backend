package com.cinnamon.service.storage;

import com.cinnamon.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@org.springframework.beans.factory.annotation.Value("${app.storage.location:uploads}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation);
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new BadRequestException("Không thể tải lên file rỗng");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // Tạo tên file ngẫu nhiên an toàn
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetDir = this.rootLocation.resolve(folder);
            Files.createDirectories(targetDir);

            Path destinationFile = targetDir.resolve(newFilename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().startsWith(targetDir.toAbsolutePath())) {
                throw new BadRequestException("Đường dẫn lưu file không hợp lệ");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Trả về URL để client có thể truy cập qua server
            return "/uploads/" + folder + "/" + newFilename;
        } catch (IOException e) {
            throw new BadRequestException("Lỗi khi lưu file: " + e.getMessage());
        }
    }
}
