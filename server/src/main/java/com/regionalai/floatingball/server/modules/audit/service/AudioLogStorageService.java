package com.regionalai.floatingball.server.modules.audit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AudioLogStorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final Path storageRoot;

    public AudioLogStorageService(@Value("${floating-ball.audit.speech-file-dir:${java.io.tmpdir}/floating-ball-server/speech-audit}") String storageRoot) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public String store(byte[] audioBytes, String originalFileName, String logId) throws IOException {
        if (audioBytes == null || audioBytes.length == 0) {
            return null;
        }
        Path targetDirectory = storageRoot.resolve(LocalDate.now().format(DATE_FORMATTER));
        Files.createDirectories(targetDirectory);

        String storedFileName = buildStoredFileName(originalFileName, logId);
        Path targetPath = targetDirectory.resolve(storedFileName).normalize();
        Files.write(targetPath, audioBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return targetPath.toString();
    }

    public void deleteQuietly(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storedPath));
        } catch (IOException ignored) {
            // 日志文件清理失败不影响主链路
        }
    }

    private String buildStoredFileName(String originalFileName, String logId) {
        String normalizedFileName = normalizeFileName(originalFileName);
        int dotIndex = normalizedFileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? normalizedFileName.substring(dotIndex) : ".bin";
        String baseName = dotIndex >= 0 ? normalizedFileName.substring(0, dotIndex) : normalizedFileName;
        return baseName + "-" + sanitizeSegment(logId) + extension;
    }

    private String normalizeFileName(String originalFileName) {
        String fallback = "speech-audio.bin";
        if (!StringUtils.hasText(originalFileName)) {
            return fallback;
        }
        try {
            String fileName = Paths.get(originalFileName.trim()).getFileName().toString();
            String sanitized = sanitizeSegment(fileName);
            return StringUtils.hasText(sanitized) ? sanitized : fallback;
        } catch (InvalidPathException ex) {
            return fallback;
        }
    }

    private String sanitizeSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "audio";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
