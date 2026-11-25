package com.afterfall.web;

import com.afterfall.web.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebControllersTest {

    @Autowired
    private SimulationApiController simulationApiController;

    @Autowired
    private StoryApiController storyApiController;

    @Autowired
    private ObserverApiController observerApiController;

    @Autowired
    private MetricsApiController metricsApiController;

    @Autowired
    private RecapApiController recapApiController;

    @Test
    @Order(1)
    @DisplayName("GET /api/state returns current simulation state")
    void testGetState() {
        ResponseEntity<StateResponse> response = simulationApiController.getState();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0L, response.getBody().tick());
        assertNotNull(response.getBody().lifecycle());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/sim/step steps one simulation tick")
    void testStepSimulation() {
        ResponseEntity<StateResponse> response = simulationApiController.step();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().tick() >= 1L);
    }

    @Test
    @DisplayName("GET /api/survivors returns initial survivor population")
    void testGetSurvivors() {
        ResponseEntity<List<SurvivorDto>> response = simulationApiController.getSurvivors();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /api/story returns current chapter and recent captions")
    void testGetStory() {
        ResponseEntity<StoryResponse> response = storyApiController.getStory();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().currentChapter());
        assertNotNull(response.getBody().recentCaptions());
    }

    @Test
    @DisplayName("GET /api/records returns observer audit records foundation")
    void testGetRecords() {
        ResponseEntity<RecordsResponse> response = observerApiController.getRecords();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().interventions());
    }

    @Test
    @DisplayName("GET /api/metrics returns shelter metrics and stockpiles")
    void testGetMetrics() {
        ResponseEntity<MetricsResponse> response = metricsApiController.getMetrics();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().aliveSurvivors() > 0);
    }

    @Test
    @DisplayName("GET /api/records/evidence returns available evidence items")
    void testGetEvidence() {
        ResponseEntity<List<EvidenceDto>> response = observerApiController.getEvidence();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("POST /api/records/evidence/{id}/discover returns 200 with discovery state")
    void testDiscoverEvidence() {
        ResponseEntity<java.util.Map<String, Object>> response = observerApiController.discoverEvidence("EV-TEST");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EV-TEST", response.getBody().get("evidenceId"));
        assertEquals(true, response.getBody().get("discovered"));
    }

    @Test
    @DisplayName("GET /api/recap returns run recap with mandatory closing line")
    void testGetRecap() {
        ResponseEntity<RecapResponse> response = recapApiController.getRecap();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().finalRecording());
        assertEquals("The settlement fell silent. The Observer remains active.", response.getBody().finalRecording().closingLine());
    }
}
