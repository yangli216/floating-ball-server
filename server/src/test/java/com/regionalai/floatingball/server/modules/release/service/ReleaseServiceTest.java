package com.regionalai.floatingball.server.modules.release.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseDownloadItem;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseHistoryView;
import com.regionalai.floatingball.server.modules.release.dto.ReleasePolicyUpdateRequest;
import com.regionalai.floatingball.server.modules.release.dto.ReleasePolicyView;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseRollbackRequest;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseUploadRequest;
import com.regionalai.floatingball.server.modules.release.dto.TauriLatestJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseServiceTest {

    @TempDir
    Path tempDir;

    private ReleaseService releaseService;

    @BeforeEach
    void setUp() {
        releaseService = new ReleaseService(tempDir.toString(), "http://release.local", new ObjectMapper());
    }

    @Test
    void uploadShouldSnapshotPreviousVersionAndRollback() {
        upload("1.2.15", "darwin-aarch64", "MedHermes_1.2.15_aarch64.app.tar.gz", false);
        upload("1.2.16", "windows-x86_64", "MedHermes_1.2.16_x64-setup.nsis.zip", true);

        TauriLatestJson latestAfterUpload = releaseService.getLatestJson("production");
        assertEquals("1.2.16", latestAfterUpload.getVersion());
        assertTrue(latestAfterUpload.getPlatforms().containsKey("windows-x86_64"));
        assertFalse(latestAfterUpload.getPlatforms().containsKey("darwin-aarch64"));

        List<ReleaseHistoryView> history = releaseService.history("production");
        assertTrue(history.stream().anyMatch(item -> "1.2.15".equals(item.getVersion())));
        assertTrue(history.stream().anyMatch(item -> "1.2.16".equals(item.getVersion()) && Boolean.TRUE.equals(item.getActive())));

        ReleaseRollbackRequest rollbackRequest = new ReleaseRollbackRequest();
        rollbackRequest.setChannel("production");
        rollbackRequest.setVersion("1.2.15");
        releaseService.rollback(rollbackRequest);

        TauriLatestJson latestAfterRollback = releaseService.getLatestJson("production");
        ReleasePolicyView policyAfterRollback = releaseService.getPolicy("production");
        assertEquals("1.2.15", latestAfterRollback.getVersion());
        assertTrue(latestAfterRollback.getPlatforms().containsKey("darwin-aarch64"));
        assertFalse(latestAfterRollback.getPlatforms().containsKey("windows-x86_64"));
        assertFalse(Boolean.TRUE.equals(policyAfterRollback.getForceUpdate()));
        assertEquals("1.2.15", policyAfterRollback.getLatestVersion());
    }

    @Test
    void updatePolicyShouldToggleForceUpdateForCurrentVersion() {
        upload("1.2.15", "darwin-aarch64", "MedHermes_1.2.15_aarch64.app.tar.gz", false);

        ReleasePolicyUpdateRequest enableRequest = new ReleasePolicyUpdateRequest();
        enableRequest.setChannel("production");
        enableRequest.setForceUpdate(true);
        releaseService.updatePolicy(enableRequest);

        ReleasePolicyView enabledPolicy = releaseService.getPolicy("production");
        assertTrue(Boolean.TRUE.equals(enabledPolicy.getForceUpdate()));
        assertEquals("1.2.15", enabledPolicy.getMinSupportedVersion());

        ReleasePolicyUpdateRequest disableRequest = new ReleasePolicyUpdateRequest();
        disableRequest.setChannel("production");
        disableRequest.setForceUpdate(false);
        releaseService.updatePolicy(disableRequest);

        ReleasePolicyView disabledPolicy = releaseService.getPolicy("production");
        assertFalse(Boolean.TRUE.equals(disabledPolicy.getForceUpdate()));
        assertNull(disabledPolicy.getMinSupportedVersion());
    }

    @Test
    void downloadItemsShouldExposeCurrentReleaseFilesForFirstInstall() {
        upload("1.2.15", "darwin-aarch64", "MedHermes_1.2.15_aarch64.app.tar.gz", false);
        upload("1.2.15", "windows-x86_64", "MedHermes_1.2.15_x64-setup.nsis.zip", false);

        List<ReleaseDownloadItem> items = releaseService.downloadItems("production", "http://release.lan:8080");

        assertEquals(2, items.size());
        ReleaseDownloadItem macItem = items.stream()
            .filter(item -> "darwin-aarch64".equals(item.getTarget()))
            .findFirst()
            .orElseThrow(AssertionError::new);
        assertEquals("1.2.15", macItem.getVersion());
        assertEquals("MedHermes_1.2.15_aarch64.app.tar.gz", macItem.getFileName());
        assertTrue(macItem.getDownloadUrl().startsWith("http://release.lan:8080/v1/client/releases/production/files/darwin-aarch64/"));
        assertTrue(macItem.getFileSize() > 0);
    }

    private void upload(String version, String target, String fileName, boolean forceUpdate) {
        ReleaseUploadRequest request = new ReleaseUploadRequest();
        request.setChannel("production");
        request.setForceUpdate(forceUpdate);
        request.setMetadataFile(new MockMultipartFile(
            "metadataFile",
            "latest.json",
            "application/json",
            buildLatestJson(version, target, fileName).getBytes(StandardCharsets.UTF_8)
        ));
        request.setFile(new MockMultipartFile(
            "file",
            fileName,
            "application/octet-stream",
            ("package-" + version).getBytes(StandardCharsets.UTF_8)
        ));
        releaseService.upload(request);
    }

    private String buildLatestJson(String version, String target, String fileName) {
        return "{"
            + "\"version\":\"" + version + "\","
            + "\"notes\":\"release " + version + "\","
            + "\"pub_date\":\"2026-04-24T10:00:00Z\","
            + "\"platforms\":{"
            + "\"" + target + "\":{"
            + "\"signature\":\"signature-" + version + "\","
            + "\"url\":\"https://example.com/" + fileName + "\""
            + "}"
            + "}"
            + "}";
    }
}
