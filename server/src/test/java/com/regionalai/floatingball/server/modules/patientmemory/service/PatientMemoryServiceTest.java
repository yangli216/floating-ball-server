package com.regionalai.floatingball.server.modules.patientmemory.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regionalai.floatingball.server.modules.device.entity.AiDevice;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryResolveRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemoryResolveResponse;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemorySyncRequest;
import com.regionalai.floatingball.server.modules.patientmemory.dto.PatientMemorySyncResponse;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemory;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemoryFact;
import com.regionalai.floatingball.server.modules.patientmemory.entity.AiPatientMemoryObservation;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryFactMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryMapper;
import com.regionalai.floatingball.server.modules.patientmemory.mapper.AiPatientMemoryObservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientMemoryServiceTest {

    @Mock
    private AiPatientMemoryMapper memoryMapper;

    @Mock
    private AiPatientMemoryObservationMapper observationMapper;

    @Mock
    private AiPatientMemoryFactMapper factMapper;

    private PatientMemoryService service;

    @BeforeEach
    void setUp() {
        service = new PatientMemoryService(memoryMapper, observationMapper, factMapper, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void syncShouldCreateVersionedMemoryAndProjectClinicalFacts() {
        List<AiPatientMemoryFact> storedFacts = new ArrayList<AiPatientMemoryFact>();
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(observationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(factMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(factMapper.insert(any(AiPatientMemoryFact.class))).thenAnswer(invocation -> {
            storedFacts.add(invocation.getArgument(0));
            return 1;
        });
        when(factMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> new ArrayList<AiPatientMemoryFact>(storedFacts));

        PatientMemorySyncRequest request = baseRequest();
        request.setObservations(Arrays.asList(
            observation("allergies", "allergy_snapshot", fact("allergy", "青霉素", "active")),
            observation("visit:V001", "visit_summary", fact("chronic_condition", "2型糖尿病", "active"))
        ));

        PatientMemorySyncResponse response = service.sync(device(), request);

        assertEquals(2, response.getAccepted());
        assertEquals(0, response.getSkipped());
        assertEquals(1L, response.getMemoryVersion());
        assertEquals("v:1", response.getNextCursor());
        assertNotNull(response.getMemoryId());
        assertEquals(1, response.getBrief().getAllergies().size());
        assertEquals("青霉素", response.getBrief().getAllergies().get(0).getName());
        assertEquals(1, response.getBrief().getChronicConditions().size());
        assertEquals("fresh", response.getBrief().getQualityStatus());
        assertTrue(response.getChangedFactTypes().contains("allergy"));
        assertTrue(response.getChangedFactTypes().contains("chronic_condition"));
        verify(memoryMapper).insert(any(AiPatientMemory.class));
        verify(observationMapper, org.mockito.Mockito.times(2)).insert(any(AiPatientMemoryObservation.class));
        verify(memoryMapper).updateById(any(AiPatientMemory.class));
    }

    @Test
    void syncShouldSkipObservationWithSameSourceAndPayloadHash() {
        AiPatientMemory existing = existingMemory(3L);
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(observationMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(factMapper.selectList(any(Wrapper.class))).thenReturn(Collections.<AiPatientMemoryFact>emptyList());

        PatientMemorySyncRequest request = baseRequest();
        request.setObservations(Collections.singletonList(
            observation("visit:V001", "visit_summary", fact("diagnosis", "高血压", "historical"))
        ));

        PatientMemorySyncResponse response = service.sync(device(), request);

        assertEquals(0, response.getAccepted());
        assertEquals(1, response.getSkipped());
        assertEquals(3L, response.getMemoryVersion());
        assertEquals("v:3", response.getNextCursor());
        verify(observationMapper, never()).insert(any(AiPatientMemoryObservation.class));
        verify(factMapper, never()).insert(any(AiPatientMemoryFact.class));
    }

    @Test
    void syncShouldRejectUnsupportedFactWithoutPersistingObservation() {
        AiPatientMemory existing = existingMemory(1L);
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(factMapper.selectList(any(Wrapper.class))).thenReturn(Collections.<AiPatientMemoryFact>emptyList());

        PatientMemorySyncRequest request = baseRequest();
        request.setObservations(Collections.singletonList(
            observation("visit:V002", "visit_summary", fact("model_guess", "疑似疾病", "unknown"))
        ));

        PatientMemorySyncResponse response = service.sync(device(), request);

        assertEquals(0, response.getAccepted());
        assertEquals(0, response.getSkipped());
        assertEquals(1, response.getRejected().size());
        assertTrue(response.getRejected().get(0).getReason().contains("factType"));
        verify(observationMapper, never()).insert(any(AiPatientMemoryObservation.class));
    }

    @Test
    void resolveShouldReturnNotModifiedWithoutLoadingFacts() {
        AiPatientMemory existing = existingMemory(7L);
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        PatientMemoryResolveRequest request = new PatientMemoryResolveRequest();
        request.setPatientId("PAT001");
        request.setHisOrgId("HIS001");
        request.setKnownMemoryVersion(7L);

        PatientMemoryResolveResponse response = service.resolve(device(), request);

        assertTrue(response.isFound());
        assertTrue(response.isNotModified());
        assertEquals(7L, response.getMemoryVersion());
        assertNull(response.getBrief());
        verify(factMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void resolveShouldReturnMissingForUnknownPatient() {
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        PatientMemoryResolveRequest request = new PatientMemoryResolveRequest();
        request.setPatientId("PAT404");
        request.setHisOrgId("HIS001");

        PatientMemoryResolveResponse response = service.resolve(device(), request);

        assertFalse(response.isFound());
        assertFalse(response.isNotModified());
        assertNull(response.getBrief());
    }

    @Test
    void syncShouldRetireFactsMissingFromUpdatedSourceSnapshot() {
        AiPatientMemory existing = existingMemory(2L);
        AiPatientMemoryFact previousAllergy = new AiPatientMemoryFact();
        previousAllergy.setIdFact("FACT001");
        previousAllergy.setIdMemory("MEM001");
        previousAllergy.setFactKey("allergy:penicillin");
        previousAllergy.setFactKeyHash("hash-penicillin");
        previousAllergy.setFactType("allergy");
        previousAllergy.setFactName("青霉素");
        previousAllergy.setFactStatus("active");
        previousAllergy.setConfidenceLevel("structured");
        previousAllergy.setSourceType("allergy_snapshot");
        previousAllergy.setSourceKey("allergies");
        previousAllergy.setOriginCode("his");
        previousAllergy.setFgSuppressed("0");
        previousAllergy.setFgActive("1");
        previousAllergy.setRevisionNo(1);

        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(observationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(factMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(previousAllergy));

        PatientMemorySyncRequest request = baseRequest();
        PatientMemorySyncRequest.Observation snapshot = new PatientMemorySyncRequest.Observation();
        snapshot.setSourceKey("allergies");
        snapshot.setSourceType("allergy_snapshot");
        snapshot.setOperation("upsert");
        snapshot.setOccurredAt(1783300000000L);
        snapshot.setFacts(Collections.<PatientMemorySyncRequest.ClinicalFact>emptyList());
        request.setObservations(Collections.singletonList(snapshot));

        PatientMemorySyncResponse response = service.sync(device(), request);

        assertEquals(1, response.getAccepted());
        assertEquals("inactive", previousAllergy.getFactStatus());
        assertTrue(response.getBrief().getAllergies().isEmpty());
        verify(factMapper).updateById(previousAllergy);
    }

    @Test
    void syncShouldTreatChangedDemographicsAsLatestHisProfileInsteadOfPermanentConflict() {
        AiPatientMemory existing = existingMemory(2L);
        when(memoryMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(factMapper.selectList(any(Wrapper.class))).thenReturn(Collections.<AiPatientMemoryFact>emptyList());

        PatientMemorySyncRequest request = baseRequest();
        request.getPatient().setGender("F");
        request.getPatient().setAgeText("59岁");
        request.setObservations(Collections.<PatientMemorySyncRequest.Observation>emptyList());

        PatientMemorySyncResponse response = service.sync(device(), request);

        assertEquals("F", existing.getPatientGender());
        assertEquals("59岁", existing.getPatientAge());
        assertEquals(0, existing.getConflictCount());
        assertEquals(3L, response.getMemoryVersion());
        assertEquals("partial", response.getBrief().getQualityStatus());
    }

    private AiDevice device() {
        AiDevice device = new AiDevice();
        device.setIdDevice("DEV001");
        device.setIdOrg("ORG001");
        device.setIdRegion("REG001");
        return device;
    }

    private PatientMemorySyncRequest baseRequest() {
        PatientMemorySyncRequest request = new PatientMemorySyncRequest();
        request.setSchemaVersion("1.0");
        request.setSyncId("SYNC001");
        PatientMemorySyncRequest.PatientIdentity patient = new PatientMemorySyncRequest.PatientIdentity();
        patient.setPatientId("PAT001");
        patient.setHisOrgId("HIS001");
        patient.setName("王某");
        patient.setGender("M");
        patient.setAgeText("58岁");
        request.setPatient(patient);
        return request;
    }

    private PatientMemorySyncRequest.Observation observation(String sourceKey,
                                                              String sourceType,
                                                              PatientMemorySyncRequest.ClinicalFact fact) {
        PatientMemorySyncRequest.Observation observation = new PatientMemorySyncRequest.Observation();
        observation.setSourceKey(sourceKey);
        observation.setSourceType(sourceType);
        observation.setSourceVersion("1");
        observation.setOperation("upsert");
        observation.setOccurredAt(1783300000000L);
        observation.setFacts(Collections.singletonList(fact));
        return observation;
    }

    private PatientMemorySyncRequest.ClinicalFact fact(String type, String name, String status) {
        PatientMemorySyncRequest.ClinicalFact fact = new PatientMemorySyncRequest.ClinicalFact();
        fact.setFactType(type);
        fact.setName(name);
        fact.setStatus(status);
        fact.setConfidence("structured");
        fact.setEvidenceText("HIS结构化记录");
        return fact;
    }

    private AiPatientMemory existingMemory(Long version) {
        AiPatientMemory memory = new AiPatientMemory();
        memory.setIdMemory("MEM001");
        memory.setIdOrg("ORG001");
        memory.setIdRegion("REG001");
        memory.setIdHisOrg("HIS001");
        memory.setPatientId("PAT001");
        memory.setPatientName("王某");
        memory.setPatientGender("M");
        memory.setPatientAge("58岁");
        memory.setMemoryVersion(version);
        memory.setConflictCount(0);
        memory.setSdStatus("active");
        memory.setFgActive("1");
        return memory;
    }
}
