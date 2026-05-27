package com.regionalai.floatingball.server.modules.analytics.mapper;

import com.regionalai.floatingball.server.modules.analytics.dto.AnalyticsQueryDTO;
import com.regionalai.floatingball.server.modules.analytics.dto.FunctionUsageQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsMapperTest {

    @Test
    void mapperShouldKeepQueryParamNamesForXmlBindings() throws Exception {
        assertTrue(AnalyticsMapper.class.isAnnotationPresent(Mapper.class));

        Method countAiService = AnalyticsMapper.class.getMethod("countAiService", AnalyticsQueryDTO.class);
        assertHasQueryParam(countAiService);

        Method functionRanking = AnalyticsMapper.class.getMethod("queryFunctionUsageRanking", FunctionUsageQueryDTO.class);
        assertHasQueryParam(functionRanking);

        Method functionTrend = AnalyticsMapper.class.getMethod("queryFunctionUsageTrend", FunctionUsageQueryDTO.class);
        assertHasQueryParam(functionTrend);
    }

    @Test
    void xmlShouldKeepPostgresAndOracleDialectBranches() throws Exception {
        String xml = mapperXml();

        assertTrue(xml.contains("_databaseId == 'postgres'"));
        assertTrue(xml.contains("DATE_TRUNC('day', event_time)"));
        assertTrue(xml.contains("CAST(CAST(change_summary_json AS jsonb) ->> 'diagnosisChanges' AS integer)"));
        assertTrue(xml.contains("JSON_VALUE(change_summary_json, '$.diagnosisChanges' RETURNING NUMBER)"));
        assertTrue(xml.contains("FROM dual"));
    }

    @Test
    void distinctModuleSqlShouldExposeCanonicalFeatureCatalog() throws Exception {
        String xml = mapperXml();

        assertTrue(xml.contains("语音问诊"));
        assertTrue(xml.contains("智能问诊"));
        assertTrue(xml.contains("AI推荐处置"));
        assertTrue(xml.contains("知识库使用"));
        assertTrue(xml.contains("VALUES"));
        assertTrue(xml.contains("UNION ALL SELECT"));
    }

    private void assertHasQueryParam(Method method) {
        Parameter parameter = method.getParameters()[0];
        Param annotation = parameter.getAnnotation(Param.class);
        assertEquals("query", annotation.value());
    }

    private String mapperXml() throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("mapper/analytics/AnalyticsMapper.xml");
        assertTrue(stream != null);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }
}
