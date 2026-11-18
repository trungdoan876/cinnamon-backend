package com.cinnamon.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * StorageService - Interface định nghĩa các hành vi lưu trữ tệp tin (upload).
 * Áp dụng tính Trừu tượng (Abstraction) trong OOP để dễ dàng thay thế
 * giữa lưu trữ Local Disk, AWS S3, Cloudinary hoặc Firebase Storage sau này.
 */
public interface StorageService {

    /**
     * Lưu trữ một tệp tin tải lên vào thư mục chỉ định.
     *
     * @param file   tệp tin Multipart tải lên từ client (ảnh, âm thanh, video)
     * @param folder thư mục con cần lưu (ví dụ: "avatars", "courses", "lessons")
     * @return đường dẫn (URL hoặc path) để truy cập tệp tin sau khi lưu
     */
    String store(MultipartFile file, String folder);
}
