package com.findit.campus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class UploadStorage {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private final Path uploadDirectory;

    public UploadStorage(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ukuran gambar maksimal 5 MB.");
        }

        String extension = switch (image.getContentType() == null ? "" : image.getContentType().toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> throw new IllegalArgumentException("Format gambar harus JPG, PNG, atau GIF.");
        };

        try {
            byte[] content = image.getBytes();
            if (ImageIO.read(new ByteArrayInputStream(content)) == null) {
                throw new IllegalArgumentException("File yang diunggah bukan gambar yang valid.");
            }
            Files.createDirectories(uploadDirectory);
            String filename = UUID.randomUUID() + extension;
            Files.write(uploadDirectory.resolve(filename), content,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return "/uploads/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Gambar tidak dapat disimpan.", exception);
        }
    }

    public void delete(String imagePath) {
        if (imagePath == null || !imagePath.startsWith("/uploads/")) {
            return;
        }
        Path file = uploadDirectory.resolve(imagePath.substring("/uploads/".length())).normalize();
        if (!file.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Lokasi gambar tidak valid.");
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Gambar tidak dapat dihapus.", exception);
        }
    }

    public Path directory() {
        return uploadDirectory;
    }
}
