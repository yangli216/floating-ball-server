package com.regionalai.floatingball.server.modules.security.service;

import com.regionalai.floatingball.server.common.db.DatabaseDialect;
import com.regionalai.floatingball.server.modules.security.entity.SecurityRejectionLog;
import com.regionalai.floatingball.server.modules.security.mapper.SecurityRejectionLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SecurityRejectionLogServiceTest {

    @Mock
    private SecurityRejectionLogMapper rejectionLogMapper;

    @Mock
    private DatabaseDialect databaseDialect;

    @Test
    void logRejectionShouldEmitErrorWhenPersistenceFails(CapturedOutput output) {
        when(rejectionLogMapper.insert(any(SecurityRejectionLog.class))).thenThrow(new RuntimeException("database unavailable"));
        SecurityRejectionLogService service = new SecurityRejectionLogService(rejectionLogMapper, databaseDialect);
        SecurityRejectionLogService.RejectionRecord record = SecurityRejectionLogService.RejectionRecord
            .of("SIG_INVALID", "POST", "/v1/ai/chat", "10.0.0.1")
            .requestId("RID-001")
            .reason("签名验证失败")
            .detail("bad signature");

        assertDoesNotThrow(() -> service.logRejection(record));

        verify(rejectionLogMapper).insert(any(SecurityRejectionLog.class));
        assertThat(output.getOut() + output.getErr())
            .contains("failed to persist security rejection log")
            .contains("SIG_INVALID")
            .contains("/v1/ai/chat")
            .contains("RID-001")
            .contains("database unavailable");
    }
}
