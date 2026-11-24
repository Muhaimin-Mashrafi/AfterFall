package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.recap.EvidenceItem;
import com.afterfall.web.dto.EvidenceDto;
import com.afterfall.web.dto.RecordsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ObserverApiController {

    private final SimulationService service;

    public ObserverApiController(SimulationService service) {
        this.service = service;
    }

    @GetMapping("/records")
    public ResponseEntity<RecordsResponse> getRecords() {
        return ResponseEntity.ok(service.getRecords());
    }

    @GetMapping("/records/evidence")
    public ResponseEntity<List<EvidenceDto>> getEvidence() {
        List<EvidenceItem> available = service.getAvailableEvidence();
        Set<String> discoveredIds = service.getDiscoveredEvidenceIds();

        List<EvidenceDto> dtos = available.stream().map(e ->
                new EvidenceDto(
                        e.evidenceId(),
                        e.tick(),
                        e.policy().name(),
                        e.target(),
                        e.originalReport(),
                        e.alteredReport(),
                        e.discrepancyType().name(),
                        e.sourceEventIds().stream().map(com.afterfall.domain.EventId::value).toList(),
                        e.referencedSurvivorIds().stream().map(com.afterfall.domain.SurvivorId::value).toList(),
                        discoveredIds.contains(e.evidenceId())
                )
        ).toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/records/evidence/{id}/discover")
    public ResponseEntity<Map<String, Object>> discoverEvidence(@PathVariable String id) {
        boolean newlyDiscovered = service.discoverEvidence(id);
        return ResponseEntity.ok(Map.of(
                "evidenceId", id,
                "discovered", true,
                "newlyDiscovered", newlyDiscovered
        ));
    }

    @PostMapping("/interventions/{id}/approve")
    public ResponseEntity<Map<String, String>> approveIntervention(@PathVariable long id) {
        // Observer in V1 proposes interventions that are audited in records
        return ResponseEntity.ok(Map.of("status", "APPROVED", "interventionId", String.valueOf(id)));
    }

    @PostMapping("/interventions/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectIntervention(@PathVariable long id) {
        return ResponseEntity.ok(Map.of("status", "REJECTED", "interventionId", String.valueOf(id)));
    }
}
