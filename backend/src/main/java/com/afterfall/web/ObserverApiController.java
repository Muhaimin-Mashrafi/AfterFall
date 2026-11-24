package com.afterfall.web;

import com.afterfall.application.SimulationService;
import com.afterfall.web.dto.RecordsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
