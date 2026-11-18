package com.cinnamon.service;

import com.cinnamon.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * LocalStorageService - Triển khai cụ thể của StorageService, thực hiện lưu trữ file trên ổ cứng máy chủ (Local Disk).
 */
@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    /**
     * Khởi tạo service lưu trữ và tự động tạo thư mục gốc nếu chưa tồn tại.
     *
     * @param storageLocation đường dẫn thư mục gốc (mặc định lấy từ cấu hình app.storage.location hoặc "uploads")
     */
    public LocalStorageService(@Value("${app.storage.location:uploads}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation);
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ file: " + storageLocation, e);
        }
    }

    /**
     * Lưu trữ tệp tin lên ổ đĩa nội bộ (Local Storage).
     * - Kiểm tra tính hợp lệ của tệp (không rỗng).
     * - Sinh tên ngẫu nhiên UUID để tránh trùng lặp tên file và ngăn chặn tấn công ghi đè.
     * - Kiểm tra an toàn chống tấn công Path Traversal (chặn ../).
     *
     * @param file   tệp tin người dùng tải lên
     * @param folder thư mục con cần lưu trữ
     * @return đường dẫn công khai bắt đầu bằng /uploads/...
     */
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

        // Tạo tên file ngẫu nhiên an toàn bằng UUID
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetDir = this.rootLocation.resolve(folder);
            Files.createDirectories(targetDir);

            Path destinationFile = targetDir.resolve(newFilename).normalize().toAbsolutePath();
            // Bảo mật chống Path Traversal: Đảm bảo file được ghi đúng trong targetDir
            if (!destinationFile.getParent().startsWith(targetDir.toAbsolutePath())) {
                throw new BadRequestException("Đường dẫn lưu file không hợp lệ");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Trả về URL để client có thể truy cập qua server static resource
            return "/uploads/" + folder + "/" + newFilename;
        } catch (IOException e) {
            throw new BadRequestException("Lỗi khi lưu file: " + e.getMessage());
        }
    }
}
