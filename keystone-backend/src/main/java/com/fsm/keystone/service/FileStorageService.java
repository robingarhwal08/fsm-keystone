package com.fsm.keystone.service;

import com.fsm.keystone.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException ex) {
            throw new BusinessException("Could not initialize upload directory");
        }
    }

    public String storeTimeLogPhoto(MultipartFile file, Long timeLogId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("Only image files are allowed");
        }

        String extension = extensionFor(file.getOriginalFilename(), contentType);
        String storedName = UUID.randomUUID() + extension;
        Path targetDir = uploadRoot.resolve("time-logs").resolve(String.valueOf(timeLogId));

        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(storedName);
            file.transferTo(target);
            return Paths.get("time-logs", String.valueOf(timeLogId), storedName)
                    .toString()
                    .replace("\\", "/");
        } catch (IOException ex) {
            throw new BusinessException("Could not store uploaded image");
        }
    }

    public Path resolve(String storagePath) {
        Path resolved = uploadRoot.resolve(storagePath).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new BusinessException("Invalid file path");
        }
        return resolved;
    }

    private String extensionFor(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
