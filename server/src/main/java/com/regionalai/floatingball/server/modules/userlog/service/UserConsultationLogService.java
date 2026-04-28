package com.regionalai.floatingball.server.modules.userlog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.exception.BusinessException;
import com.regionalai.floatingball.server.modules.audit.service.AudioLogStorageService;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.userlog.dto.UserConsultationLogListItem;
import com.regionalai.floatingball.server.modules.userlog.dto.UserConsultationLogRequest;
import com.regionalai.floatingball.server.modules.userlog.entity.AiUserConsultationLog;
import com.regionalai.floatingball.server.modules.userlog.mapper.AiUserConsultationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class UserConsultationLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String STATUS_GENERATED = "generated";
    private static final String STATUS_COMPLETED = "completed";

    private final AiUserConsultationLogMapper userConsultationLogMapper;
    private final ObjectMapper objectMapper;
    private final AudioLogStorageService audioLogStorageService;

    public UserConsultationLogService(AiUserConsultationLogMapper userConsultationLogMapper,
                                      ObjectMapper objectMapper,
                                      AudioLogStorageService audioLogStorageService) {
        this.userConsultationLogMapper = userConsultationLogMapper;
        this.objectMapper = objectMapper;
        this.audioLogStorageService = audioLogStorageService;
    }

    public AiUserConsultationLog save(AiDevice device, UserConsultationLogRequest request) {
        if (request == null) {
            throw new BusinessException("用户日志请求不能为空");
        }
        String consultationId = trimToNull(request.getConsultationId());
        if (consultationId == null) {
            throw new BusinessException("问诊ID不能为空");
        }
        String consultationType = normalizeConsultationType(request.getConsultationType());

        AiUserConsultationLog entity = findExisting(device, consultationId, consultationType);
        boolean create = entity == null;
        if (create) {
            entity = new AiUserConsultationLog();
            entity.setConsultationId(consultationId);
            entity.setConsultationType(consultationType);
            entity.setIdDevice(device == null ? null : device.getIdDevice());
            entity.setFgActive("1");
            entity.setStatus(STATUS_GENERATED);
            entity.setIdLog(UUID.randomUUID().toString().replace("-", ""));
        }

        fillCommonFields(entity, device, request);
        fillSpeechText(entity, request);

        if (request.getFirstSnapshot() != null && !STATUS_COMPLETED.equals(entity.getStatus())) {
            entity.setFirstSnapshotJson(writeJson(request.getFirstSnapshot()));
        }
        if (request.getFinalSnapshot() != null) {
            entity.setFinalSnapshotJson(writeJson(request.getFinalSnapshot()));
            entity.setStatus(STATUS_COMPLETED);
        } else if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_GENERATED);
        }
        if (request.getSelectionSnapshot() != null) {
            entity.setSelectionJson(writeJson(request.getSelectionSnapshot()));
        }

        String previousAudioPath = entity.getAudioFilePath();
        String storedAudioPath = storeAudioIfPresent(entity, request);
        try {
            if (create) {
                userConsultationLogMapper.insert(entity);
            } else {
                userConsultationLogMapper.updateById(entity);
            }
            if (storedAudioPath != null && StringUtils.hasText(previousAudioPath)
                && !storedAudioPath.equals(previousAudioPath)) {
                audioLogStorageService.deleteQuietly(previousAudioPath);
            }
        } catch (RuntimeException ex) {
            audioLogStorageService.deleteQuietly(storedAudioPath);
            throw ex;
        }
        return entity;
    }

    public PageResponse<UserConsultationLogListItem> list(long current,
                                                          long size,
                                                          String keyword,
                                                          String consultationType,
                                                          String dateFrom,
                                                          String dateTo) {
        Page<AiUserConsultationLog> page = new Page<AiUserConsultationLog>(current, size);
        LocalDateTime startTime = parseDateTime(dateFrom, false);
        LocalDateTime endTime = parseDateTime(dateTo, true);
        LambdaQueryWrapper<AiUserConsultationLog> wrapper = new LambdaQueryWrapper<AiUserConsultationLog>()
            .eq(AiUserConsultationLog::getFgActive, "1")
            .orderByDesc(AiUserConsultationLog::getConsultationTime);

        if (StringUtils.hasText(keyword)) {
            String text = keyword.trim();
            wrapper.and(q -> q
                .like(AiUserConsultationLog::getNaOrg, text)
                .or()
                .like(AiUserConsultationLog::getIdOrg, text)
                .or()
                .like(AiUserConsultationLog::getNaDoctor, text)
                .or()
                .like(AiUserConsultationLog::getIdDoctor, text)
                .or()
                .like(AiUserConsultationLog::getPatientName, text)
                .or()
                .like(AiUserConsultationLog::getPatientId, text)
                .or()
                .like(AiUserConsultationLog::getConsultationId, text));
        }
        if (StringUtils.hasText(consultationType)) {
            wrapper.eq(AiUserConsultationLog::getConsultationType, normalizeConsultationType(consultationType));
        }
        if (startTime != null) {
            wrapper.ge(AiUserConsultationLog::getConsultationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AiUserConsultationLog::getConsultationTime, endTime);
        }

        Page<AiUserConsultationLog> result = userConsultationLogMapper.selectPage(page, wrapper);
        return new PageResponse<UserConsultationLogListItem>(
            result.getCurrent(),
            result.getSize(),
            result.getTotal(),
            toListItems(result.getRecords())
        );
    }

    public AiUserConsultationLog detail(String idLog) {
        if (!StringUtils.hasText(idLog)) {
            throw new BusinessException("用户日志ID不能为空");
        }
        AiUserConsultationLog log = userConsultationLogMapper.selectById(idLog);
        if (log == null || !"1".equals(log.getFgActive())) {
            throw new BusinessException("用户日志不存在");
        }
        return log;
    }

    public AudioFile resolveAudioFile(String idLog) {
        AiUserConsultationLog log = detail(idLog);
        if (!StringUtils.hasText(log.getAudioFilePath())) {
            throw new BusinessException("该用户日志没有录音文件");
        }
        try {
            return new AudioFile(
                audioLogStorageService.resolveExistingPath(log.getAudioFilePath()),
                log.getAudioMimeType(),
                log.getAudioFileName()
            );
        } catch (IOException ex) {
            throw new BusinessException("录音文件不可用：" + ex.getMessage());
        }
    }

    private AiUserConsultationLog findExisting(AiDevice device, String consultationId, String consultationType) {
        LambdaQueryWrapper<AiUserConsultationLog> wrapper = new LambdaQueryWrapper<AiUserConsultationLog>()
            .eq(AiUserConsultationLog::getFgActive, "1")
            .eq(AiUserConsultationLog::getConsultationId, consultationId)
            .eq(AiUserConsultationLog::getConsultationType, consultationType);
        if (device != null && StringUtils.hasText(device.getIdDevice())) {
            wrapper.eq(AiUserConsultationLog::getIdDevice, device.getIdDevice());
        }
        return userConsultationLogMapper.selectOne(wrapper.last("FETCH FIRST 1 ROWS ONLY"));
    }

    private void fillCommonFields(AiUserConsultationLog entity, AiDevice device, UserConsultationLogRequest request) {
        entity.setIdOrg(firstNonBlank(device == null ? null : device.getIdOrg(), request.getOrgCode(), entity.getIdOrg()));
        entity.setNaOrg(firstNonBlank(request.getOrgName(), entity.getNaOrg()));
        entity.setIdDoctor(firstNonBlank(request.getDoctorId(), entity.getIdDoctor()));
        entity.setNaDoctor(firstNonBlank(request.getDoctorName(), entity.getNaDoctor()));
        entity.setIdDept(firstNonBlank(request.getDeptId(), entity.getIdDept()));
        entity.setNaDept(firstNonBlank(request.getDeptName(), entity.getNaDept()));
        entity.setPatientId(firstNonBlank(request.getPatientId(), entity.getPatientId()));
        entity.setPatientName(firstNonBlank(request.getPatientName(), entity.getPatientName()));
        entity.setPatientGender(firstNonBlank(request.getPatientGender(), entity.getPatientGender()));
        entity.setPatientAge(firstNonBlank(request.getPatientAge(), entity.getPatientAge()));
        LocalDateTime requestTime = parseEpochMillis(request.getConsultationTime());
        if (requestTime != null) {
            entity.setConsultationTime(requestTime);
        } else if (entity.getConsultationTime() == null) {
            entity.setConsultationTime(LocalDateTime.now());
        }
    }

    private void fillSpeechText(AiUserConsultationLog entity, UserConsultationLogRequest request) {
        String speechText = trimToNull(request.getSpeechText());
        if (speechText != null) {
            entity.setSpeechText(speechText);
        }
    }

    private String storeAudioIfPresent(AiUserConsultationLog entity, UserConsultationLogRequest request) {
        String audio = trimToNull(request.getAudio());
        if (audio == null) {
            return null;
        }
        byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(stripDataUrlPrefix(audio));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("用户日志录音内容不是合法 Base64");
        }
        if (audioBytes.length == 0) {
            return null;
        }
        String fileName = resolveAudioFileName(request);
        try {
            String storedPath = audioLogStorageService.store(
                audioBytes,
                fileName,
                entity.getIdLog() + "-" + System.currentTimeMillis()
            );
            entity.setAudioFilePath(storedPath);
            entity.setAudioFileName(fileName);
            entity.setAudioMimeType(firstNonBlank(request.getAudioMimeType(), resolveAudioMimeType(fileName)));
            entity.setAudioSize((long) audioBytes.length);
            return storedPath;
        } catch (IOException ex) {
            throw new BusinessException("用户日志录音保存失败：" + ex.getMessage());
        }
    }

    private String stripDataUrlPrefix(String value) {
        int commaIndex = value.indexOf(',');
        if (value.startsWith("data:") && commaIndex >= 0) {
            return value.substring(commaIndex + 1);
        }
        return value;
    }

    private String resolveAudioFileName(UserConsultationLogRequest request) {
        String fileName = trimToNull(request.getAudioFileName());
        if (fileName != null) {
            return fileName;
        }
        String extension = resolveAudioExtension(request.getAudioMimeType(), request.getAudioFormat());
        return "voice-consultation-" + System.currentTimeMillis() + extension;
    }

    private String resolveAudioExtension(String mimeType, String format) {
        String normalizedMimeType = trimToNull(mimeType);
        String normalizedFormat = trimToNull(format);
        if ("audio/wav".equalsIgnoreCase(normalizedMimeType) || "audio/wave".equalsIgnoreCase(normalizedMimeType)) {
            return ".wav";
        }
        if ("audio/webm".equalsIgnoreCase(normalizedMimeType)) {
            return ".webm";
        }
        if ("audio/mpeg".equalsIgnoreCase(normalizedMimeType)) {
            return ".mp3";
        }
        if ("audio/mp4".equalsIgnoreCase(normalizedMimeType)) {
            return ".m4a";
        }
        if ("audio/ogg".equalsIgnoreCase(normalizedMimeType)) {
            return ".ogg";
        }
        if ("audio/pcm".equalsIgnoreCase(normalizedMimeType) || "pcm".equalsIgnoreCase(normalizedFormat)) {
            return ".pcm";
        }
        return ".bin";
    }

    private String resolveAudioMimeType(String fileName) {
        String normalized = fileName == null ? "" : fileName.toLowerCase();
        if (normalized.endsWith(".wav")) {
            return "audio/wav";
        }
        if (normalized.endsWith(".webm")) {
            return "audio/webm";
        }
        if (normalized.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (normalized.endsWith(".m4a")) {
            return "audio/mp4";
        }
        if (normalized.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (normalized.endsWith(".pcm")) {
            return "audio/pcm";
        }
        return null;
    }

    private String normalizeConsultationType(String value) {
        String text = trimToNull(value);
        if (text == null) {
            throw new BusinessException("问诊类型不能为空");
        }
        String normalized = text.toLowerCase();
        if (!"voice".equals(normalized) && !"smart".equals(normalized)) {
            throw new BusinessException("问诊类型非法");
        }
        return normalized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("用户日志快照序列化失败");
        }
    }

    private LocalDateTime parseEpochMillis(Long value) {
        if (value == null || value <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }

    private List<UserConsultationLogListItem> toListItems(List<AiUserConsultationLog> records) {
        List<UserConsultationLogListItem> items = new ArrayList<UserConsultationLogListItem>();
        if (records == null) {
            return items;
        }
        for (AiUserConsultationLog record : records) {
            UserConsultationLogListItem item = new UserConsultationLogListItem();
            item.setIdLog(record.getIdLog());
            item.setConsultationId(record.getConsultationId());
            item.setIdOrg(record.getIdOrg());
            item.setNaOrg(record.getNaOrg());
            item.setIdDoctor(record.getIdDoctor());
            item.setNaDoctor(record.getNaDoctor());
            item.setConsultationType(record.getConsultationType());
            item.setConsultationTime(record.getConsultationTime());
            item.setPatientId(record.getPatientId());
            item.setPatientName(record.getPatientName());
            item.setPatientGender(record.getPatientGender());
            item.setPatientAge(record.getPatientAge());
            item.setHasAudio(record.getHasAudio());
            item.setHasSpeechText(record.getHasSpeechText());
            item.setStatus(record.getStatus());
            items.add(item);
        }
        return items;
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String candidate = value.trim();
        try {
            if (candidate.length() == 10) {
                LocalDate date = LocalDate.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            return LocalDateTime.parse(candidate, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(candidate, ISO_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(candidate).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            throw new BusinessException("用户日志查询时间格式非法");
        }
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String text = trimToNull(candidate);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public static class AudioFile {

        private final Path path;
        private final String mimeType;
        private final String fileName;

        public AudioFile(Path path, String mimeType, String fileName) {
            this.path = path;
            this.mimeType = mimeType;
            this.fileName = fileName;
        }

        public Path getPath() {
            return path;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
