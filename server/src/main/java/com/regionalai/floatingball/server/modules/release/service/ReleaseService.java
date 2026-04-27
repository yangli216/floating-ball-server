package com.regionalai.floatingball.server.modules.release.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseUploadRequest;
import com.regionalai.floatingball.server.modules.release.dto.ReleaseView;
import com.regionalai.floatingball.server.modules.release.dto.TauriLatestJson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ReleaseService {

    private static final List<String> CHANNELS = Arrays.asList("production", "testing");
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final String LATEST_FILE = "latest.json";

    private final Path storageRoot;
    private final ObjectMapper objectMapper;
    private final String publicBaseUrl;

    public ReleaseService(@Value("${floating-ball.release.storage-dir:${java.io.tmpdir}/floating-ball-server/releases}") String storageRoot,
                          @Value("${floating-ball.release.public-base-url:}") String publicBaseUrl,
                          ObjectMapper objectMapper) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
        this.objectMapper = objectMapper;
    }

    public List<ReleaseView> list(String channel) {
        if (StringUtils.hasText(channel)) {
            return Collections.singletonList(readReleaseView(normalizeChannel(channel)));
        }

        List<ReleaseView> views = new ArrayList<ReleaseView>();
        for (String item : CHANNELS) {
            views.add(readReleaseView(item));
        }
        return views;
    }

    public ReleaseView upload(ReleaseUploadRequest request) {
        String channel = normalizeChannel(request.getChannel());
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("安装包文件不能为空");
        }

        String originalFileName = safeFileName(file.getOriginalFilename());
        TauriLatestJson uploadedLatestJson = readUploadedLatestJson(request.getMetadataFile());
        ReleaseMetadata releaseMetadata = resolveReleaseMetadata(request, uploadedLatestJson, originalFileName);
        String version = releaseMetadata.version;
        String target = releaseMetadata.target;
        String signature = releaseMetadata.signature;
        String pubDate = releaseMetadata.pubDate;
        String notes = releaseMetadata.notes;
        validatePackageMatchesMetadata(uploadedLatestJson, originalFileName, releaseMetadata);
        Path targetDirectory = storageRoot.resolve(channel).resolve(target).normalize();
        ensureInsideStorage(targetDirectory);

        try {
            Files.createDirectories(targetDirectory);
            Path targetPath = targetDirectory.resolve(originalFileName).normalize();
            ensureInsideStorage(targetPath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            TauriLatestJson latestJson = readLatestJson(channel);
            latestJson.setVersion(version);
            latestJson.setNotes(notes);
            latestJson.setPubDate(pubDate);
            TauriLatestJson.PlatformInfo platformInfo = new TauriLatestJson.PlatformInfo();
            platformInfo.setSignature(signature);
            platformInfo.setUrl(buildFileUrl(channel, target, originalFileName));
            latestJson.getPlatforms().put(target, platformInfo);
            writeLatestJson(channel, latestJson);

            return toReleaseView(channel, version, target, originalFileName, Files.size(targetPath), platformInfo.getUrl(), pubDate, notes);
        } catch (IOException ex) {
            throw new BusinessException("RELEASE-IO", "保存发布文件失败: " + ex.getMessage());
        }
    }

    private TauriLatestJson readUploadedLatestJson(MultipartFile metadataFile) {
        if (metadataFile == null || metadataFile.isEmpty()) {
            return null;
        }
        try (InputStream inputStream = metadataFile.getInputStream()) {
            return objectMapper.readValue(inputStream, TauriLatestJson.class);
        } catch (IOException ex) {
            throw new BusinessException("RELEASE-JSON", "解析 latest.json 失败: " + ex.getMessage());
        }
    }

    private ReleaseMetadata resolveReleaseMetadata(ReleaseUploadRequest request,
                                                   TauriLatestJson uploadedLatestJson,
                                                   String originalFileName) {
        String version = trimToNull(request.getVersion());
        String target = trimToNull(request.getTarget());
        String signature = trimToNull(request.getSignature());
        String notes = trimToNull(request.getNotes());
        String pubDate = trimToNull(request.getPubDate());

        if (uploadedLatestJson != null) {
            version = firstText(version, uploadedLatestJson.getVersion());
            notes = firstText(notes, uploadedLatestJson.getNotes());
            pubDate = firstText(pubDate, uploadedLatestJson.getPubDate());

            PlatformMatch platformMatch = findPlatformMatch(uploadedLatestJson, originalFileName, target);
            if (platformMatch != null) {
                target = firstText(target, platformMatch.target);
                signature = firstText(signature, platformMatch.platformInfo.getSignature());
            }
        }

        version = requireSafeText(version, "版本号不能为空，请上传 latest.json 或手工填写版本号", "版本号只能包含字母、数字、点、下划线和短横线");
        target = requireSafeText(target, "平台 target 不能为空，请上传可匹配安装包的 latest.json 或手工填写 target", "平台 target 只能包含字母、数字、点、下划线和短横线");
        signature = requireText(signature, "签名不能为空，请上传包含当前安装包签名的 latest.json");

        ReleaseMetadata metadata = new ReleaseMetadata();
        metadata.version = version;
        metadata.target = target;
        metadata.signature = signature;
        metadata.notes = notes;
        metadata.pubDate = normalizePubDate(pubDate);
        return metadata;
    }

    private void validatePackageMatchesMetadata(TauriLatestJson uploadedLatestJson,
                                                String originalFileName,
                                                ReleaseMetadata metadata) {
        if (uploadedLatestJson == null) {
            return;
        }
        TauriLatestJson.PlatformInfo platformInfo = uploadedLatestJson.getPlatforms().get(metadata.target);
        if (platformInfo == null || !StringUtils.hasText(platformInfo.getUrl())) {
            return;
        }
        String signedFileName = extractFileName(platformInfo.getUrl());
        if (!originalFileName.equals(signedFileName)) {
            throw new BusinessException(
                "上传的安装包与 latest.json 中 target=" + metadata.target + " 的签名文件不一致：latest.json 签名对应 "
                    + signedFileName + "，当前上传 " + originalFileName + "。请上传 latest.json 里 url 指向的同名文件，否则 Tauri 会签名校验失败。"
            );
        }
    }

    private PlatformMatch findPlatformMatch(TauriLatestJson latestJson, String originalFileName, String preferredTarget) {
        if (latestJson == null || latestJson.getPlatforms() == null || latestJson.getPlatforms().isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(preferredTarget) && latestJson.getPlatforms().containsKey(preferredTarget)) {
            return new PlatformMatch(preferredTarget, latestJson.getPlatforms().get(preferredTarget));
        }
        for (String target : latestJson.getPlatforms().keySet()) {
            TauriLatestJson.PlatformInfo platformInfo = latestJson.getPlatforms().get(target);
            if (originalFileName.equals(extractFileName(platformInfo.getUrl()))) {
                return new PlatformMatch(target, platformInfo);
            }
        }
        if (latestJson.getPlatforms().size() == 1) {
            String target = latestJson.getPlatforms().keySet().iterator().next();
            return new PlatformMatch(target, latestJson.getPlatforms().get(target));
        }
        throw new BusinessException("latest.json 中包含多个平台，但无法根据安装包文件名匹配；请在平台 target 中选择正确项");
    }

    public TauriLatestJson getLatestJson(String channel) {
        return getLatestJson(channel, null);
    }

    public TauriLatestJson getLatestJson(String channel, String baseUrl) {
        String normalizedChannel = normalizeChannel(channel);
        TauriLatestJson latestJson = readLatestJson(normalizedChannel);
        if (!StringUtils.hasText(latestJson.getVersion()) || latestJson.getPlatforms().isEmpty()) {
            throw new BusinessException("RELEASE-404", "当前通道暂无可用版本");
        }
        if (StringUtils.hasText(baseUrl)) {
            rewriteDownloadUrls(latestJson, normalizedChannel, baseUrl.trim().replaceAll("/+$", ""));
        }
        return latestJson;
    }

    private void rewriteDownloadUrls(TauriLatestJson latestJson, String channel, String baseUrl) {
        for (String target : latestJson.getPlatforms().keySet()) {
            TauriLatestJson.PlatformInfo platformInfo = latestJson.getPlatforms().get(target);
            String fileName = extractFileName(platformInfo.getUrl());
            if (StringUtils.hasText(fileName)) {
                platformInfo.setUrl(baseUrl + "/v1/client/releases/" + channel + "/files/" + target + "/" + fileName);
            }
        }
    }

    public Path resolveFile(String channel, String target, String fileName) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedTarget = requireSafeText(target, "平台 target 不能为空", "平台 target 非法");
        String normalizedFileName = safeFileName(fileName);
        Path filePath = storageRoot.resolve(normalizedChannel).resolve(normalizedTarget).resolve(normalizedFileName).normalize();
        ensureInsideStorage(filePath);
        if (!Files.isRegularFile(filePath)) {
            throw new BusinessException("RELEASE-404", "安装包文件不存在");
        }
        return filePath;
    }

    private ReleaseView readReleaseView(String channel) {
        TauriLatestJson latestJson = readLatestJson(channel);
        if (!StringUtils.hasText(latestJson.getVersion()) || latestJson.getPlatforms().isEmpty()) {
            ReleaseView empty = new ReleaseView();
            empty.setChannel(channel);
            empty.setLatestJsonUrl(buildLatestJsonUrl(channel));
            return empty;
        }

        String target = latestJson.getPlatforms().keySet().iterator().next();
        TauriLatestJson.PlatformInfo platformInfo = latestJson.getPlatforms().get(target);
        String fileName = extractFileName(platformInfo.getUrl());
        Long fileSize = null;
        if (StringUtils.hasText(fileName)) {
            Path filePath = storageRoot.resolve(channel).resolve(target).resolve(fileName).normalize();
            if (Files.isRegularFile(filePath)) {
                try {
                    fileSize = Files.size(filePath);
                } catch (IOException ignored) {
                    fileSize = null;
                }
            }
        }
        return toReleaseView(channel, latestJson.getVersion(), target, fileName, fileSize, platformInfo.getUrl(), latestJson.getPubDate(), latestJson.getNotes());
    }

    private TauriLatestJson readLatestJson(String channel) {
        Path latestPath = latestJsonPath(channel);
        if (!Files.isRegularFile(latestPath)) {
            return new TauriLatestJson();
        }
        try {
            return objectMapper.readValue(latestPath.toFile(), TauriLatestJson.class);
        } catch (IOException ex) {
            throw new BusinessException("RELEASE-JSON", "读取 latest.json 失败: " + ex.getMessage());
        }
    }

    private void writeLatestJson(String channel, TauriLatestJson latestJson) throws IOException {
        Path channelDirectory = storageRoot.resolve(channel).normalize();
        ensureInsideStorage(channelDirectory);
        Files.createDirectories(channelDirectory);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(latestJsonPath(channel).toFile(), latestJson);
    }

    private Path latestJsonPath(String channel) {
        Path latestPath = storageRoot.resolve(channel).resolve(LATEST_FILE).normalize();
        ensureInsideStorage(latestPath);
        return latestPath;
    }

    private ReleaseView toReleaseView(String channel,
                                      String version,
                                      String target,
                                      String fileName,
                                      Long fileSize,
                                      String downloadUrl,
                                      String pubDate,
                                      String notes) {
        ReleaseView view = new ReleaseView();
        view.setChannel(channel);
        view.setVersion(version);
        view.setTarget(target);
        view.setFileName(fileName);
        view.setFileSize(fileSize);
        view.setDownloadUrl(downloadUrl);
        view.setLatestJsonUrl(buildLatestJsonUrl(channel));
        view.setPubDate(pubDate);
        view.setNotes(notes);
        view.setUpdatedAt(System.currentTimeMillis());
        return view;
    }

    private String normalizeChannel(String channel) {
        String value = requireText(channel, "发布通道不能为空").trim();
        if (!CHANNELS.contains(value)) {
            throw new BusinessException("发布通道仅支持 production 或 testing");
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String requireSafeText(String value, String emptyMessage, String invalidMessage) {
        String text = requireText(value, emptyMessage);
        if (!SAFE_SEGMENT.matcher(text).matches()) {
            throw new BusinessException(invalidMessage);
        }
        return text;
    }

    private String safeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("文件名不能为空");
        }
        try {
            String fileName = Paths.get(value.trim()).getFileName().toString();
            if (!SAFE_SEGMENT.matcher(fileName).matches()) {
                throw new BusinessException("文件名只能包含字母、数字、点、下划线和短横线");
            }
            return fileName;
        } catch (InvalidPathException ex) {
            throw new BusinessException("文件名非法");
        }
    }

    private String normalizePubDate(String pubDate) {
        if (!StringUtils.hasText(pubDate)) {
            return Instant.now().toString();
        }
        try {
            return Instant.parse(pubDate.trim()).toString();
        } catch (DateTimeParseException ex) {
            throw new BusinessException("发布时间必须是 ISO-8601 格式，例如 2026-04-24T10:00:00Z");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : trimToNull(fallback);
    }

    private void ensureInsideStorage(Path path) {
        if (!path.normalize().startsWith(storageRoot)) {
            throw new BusinessException("文件路径非法");
        }
    }

    private String buildLatestJsonUrl(String channel) {
        return externalBaseUrlBuilder()
            .path("/v1/client/releases/")
            .path(channel)
            .path("/latest.json")
            .toUriString();
    }

    private String buildFileUrl(String channel, String target, String fileName) {
        return externalBaseUrlBuilder()
            .path("/v1/client/releases/")
            .path(channel)
            .path("/files/")
            .path(target)
            .path("/")
            .path(fileName)
            .toUriString();
    }

    public String normalizeExternalBaseUrl(String requestBaseUrl) {
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl;
        }
        String baseUrl = trimTrailingSlash(requestBaseUrl);
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        try {
            java.net.URI uri = java.net.URI.create(baseUrl);
            String host = uri.getHost();
            if (!isLoopbackHost(host)) {
                return baseUrl;
            }
            String lanIp = findLanIp();
            if (!StringUtils.hasText(lanIp)) {
                return baseUrl;
            }
            int port = uri.getPort();
            String portPart = port > 0 ? ":" + port : "";
            return uri.getScheme() + "://" + lanIp + portPart;
        } catch (RuntimeException ex) {
            return baseUrl;
        }
    }

    private UriComponentsBuilder externalBaseUrlBuilder() {
        if (StringUtils.hasText(publicBaseUrl)) {
            return ServletUriComponentsBuilder.fromHttpUrl(publicBaseUrl);
        }
        String currentContext = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        return ServletUriComponentsBuilder.fromHttpUrl(normalizeExternalBaseUrl(currentContext));
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
            || "127.0.0.1".equals(host)
            || "::1".equals(host)
            || "0:0:0:0:0:0:0:1".equals(host);
    }

    private String findLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String hostAddress = address.getHostAddress();
                        if (isUsableLanIp(hostAddress)) {
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private boolean isUsableLanIp(String hostAddress) {
        return StringUtils.hasText(hostAddress)
            && !hostAddress.startsWith("169.254.")
            && !hostAddress.startsWith("198.18.")
            && !hostAddress.startsWith("198.19.")
            && !hostAddress.startsWith("100.64.")
            && !hostAddress.startsWith("100.65.")
            && !hostAddress.startsWith("100.66.")
            && !hostAddress.startsWith("100.67.")
            && !hostAddress.startsWith("100.68.")
            && !hostAddress.startsWith("100.69.")
            && !hostAddress.startsWith("100.70.")
            && !hostAddress.startsWith("100.71.")
            && !hostAddress.startsWith("100.72.")
            && !hostAddress.startsWith("100.73.")
            && !hostAddress.startsWith("100.74.")
            && !hostAddress.startsWith("100.75.")
            && !hostAddress.startsWith("100.76.")
            && !hostAddress.startsWith("100.77.")
            && !hostAddress.startsWith("100.78.")
            && !hostAddress.startsWith("100.79.")
            && !hostAddress.startsWith("100.80.")
            && !hostAddress.startsWith("100.81.")
            && !hostAddress.startsWith("100.82.")
            && !hostAddress.startsWith("100.83.")
            && !hostAddress.startsWith("100.84.")
            && !hostAddress.startsWith("100.85.")
            && !hostAddress.startsWith("100.86.")
            && !hostAddress.startsWith("100.87.")
            && !hostAddress.startsWith("100.88.")
            && !hostAddress.startsWith("100.89.")
            && !hostAddress.startsWith("100.90.")
            && !hostAddress.startsWith("100.91.")
            && !hostAddress.startsWith("100.92.")
            && !hostAddress.startsWith("100.93.")
            && !hostAddress.startsWith("100.94.")
            && !hostAddress.startsWith("100.95.")
            && !hostAddress.startsWith("100.96.")
            && !hostAddress.startsWith("100.97.")
            && !hostAddress.startsWith("100.98.")
            && !hostAddress.startsWith("100.99.")
            && !hostAddress.startsWith("100.100.")
            && !hostAddress.startsWith("100.101.")
            && !hostAddress.startsWith("100.102.")
            && !hostAddress.startsWith("100.103.")
            && !hostAddress.startsWith("100.104.")
            && !hostAddress.startsWith("100.105.")
            && !hostAddress.startsWith("100.106.")
            && !hostAddress.startsWith("100.107.")
            && !hostAddress.startsWith("100.108.")
            && !hostAddress.startsWith("100.109.")
            && !hostAddress.startsWith("100.110.")
            && !hostAddress.startsWith("100.111.")
            && !hostAddress.startsWith("100.112.")
            && !hostAddress.startsWith("100.113.")
            && !hostAddress.startsWith("100.114.")
            && !hostAddress.startsWith("100.115.")
            && !hostAddress.startsWith("100.116.")
            && !hostAddress.startsWith("100.117.")
            && !hostAddress.startsWith("100.118.")
            && !hostAddress.startsWith("100.119.")
            && !hostAddress.startsWith("100.120.")
            && !hostAddress.startsWith("100.121.")
            && !hostAddress.startsWith("100.122.")
            && !hostAddress.startsWith("100.123.")
            && !hostAddress.startsWith("100.124.")
            && !hostAddress.startsWith("100.125.")
            && !hostAddress.startsWith("100.126.")
            && !hostAddress.startsWith("100.127.");
    }

    private String extractFileName(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        int index = url.lastIndexOf('/');
        return index >= 0 && index < url.length() - 1 ? url.substring(index + 1) : url;
    }

    private static class ReleaseMetadata {
        private String version;
        private String target;
        private String signature;
        private String notes;
        private String pubDate;
    }

    private static class PlatformMatch {
        private final String target;
        private final TauriLatestJson.PlatformInfo platformInfo;

        private PlatformMatch(String target, TauriLatestJson.PlatformInfo platformInfo) {
            this.target = target;
            this.platformInfo = platformInfo;
        }
    }
}
